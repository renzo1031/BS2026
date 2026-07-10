package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.service.common.AuthContext;
import com.campus.service.common.BusinessException;
import com.campus.service.common.PageResult;
import com.campus.service.domain.RequestRules;
import com.campus.service.domain.RequestStatus;
import com.campus.service.entity.Feedback;
import com.campus.service.entity.ProcessRecord;
import com.campus.service.entity.ServiceItem;
import com.campus.service.entity.ServiceRequest;
import com.campus.service.entity.User;
import com.campus.service.entity.Venue;
import com.campus.service.mapper.FeedbackMapper;
import com.campus.service.mapper.ProcessRecordMapper;
import com.campus.service.mapper.ServiceItemMapper;
import com.campus.service.mapper.ServiceRequestMapper;
import com.campus.service.mapper.UserMapper;
import com.campus.service.mapper.VenueMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RequestService {
    private static final DateTimeFormatter CERTIFICATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ServiceRequestMapper requestMapper;
    private final ServiceItemMapper itemMapper;
    private final UserMapper userMapper;
    private final VenueMapper venueMapper;
    private final ProcessRecordMapper recordMapper;
    private final FeedbackMapper feedbackMapper;
    private final NoticeService noticeService;
    private final LogService logService;

    public RequestService(ServiceRequestMapper requestMapper, ServiceItemMapper itemMapper, UserMapper userMapper,
                          VenueMapper venueMapper, ProcessRecordMapper recordMapper, FeedbackMapper feedbackMapper,
                          NoticeService noticeService, LogService logService) {
        this.requestMapper = requestMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.venueMapper = venueMapper;
        this.recordMapper = recordMapper;
        this.feedbackMapper = feedbackMapper;
        this.noticeService = noticeService;
        this.logService = logService;
    }

    @Transactional
    public ServiceRequest create(CreateRequestCommand command) {
        AuthContext.AuthUser auth = AuthContext.get();
        if (!auth.hasRole("STUDENT")) {
            throw new BusinessException(403, "只有学生可以提交申请");
        }
        if (command == null || command.itemId() == null) {
            throw new BusinessException("服务事项不能为空");
        }

        ServiceItem item = itemMapper.selectById(command.itemId());
        if (item == null) {
            throw new BusinessException(404, "服务事项不存在");
        }
        if (!Integer.valueOf(1).equals(item.getEnabled())) {
            throw new BusinessException("服务事项已停用");
        }

        LocalDateTime now = LocalDateTime.now();
        ServiceRequest request = new ServiceRequest();
        request.setRequestNo(UUID.randomUUID().toString());
        request.setVersion(0);
        request.setItemId(item.getId());
        request.setApplicantId(auth.userId());
        request.setDepartmentId(item.getDepartmentId());
        request.setTitle(RequestRules.clean(command.title()));
        request.setContent(RequestRules.clean(command.content()));
        request.setStatus(RequestStatus.SUBMITTED.name());
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        request.setDeleted(0);
        RequestRules.validateCommon(request);

        switch (item.getType()) {
            case "REPAIR" -> prepareRepair(request, command);
            case "CERTIFICATE" -> prepareCertificate(request, command);
            case "VENUE" -> prepareVenue(request, command, now);
            default -> throw new BusinessException("不支持的服务事项类型");
        }

        requestMapper.insert(request);
        record(request, null, RequestStatus.SUBMITTED.name(), "提交申请", "学生提交服务申请");
        logService.log("REQUEST", "CREATE", request.getRequestNo());
        return request;
    }

    public PageResult<ServiceRequest> my(String status, long page, long size) {
        RequestRules.validatePage(page, size);
        QueryWrapper<ServiceRequest> query = new QueryWrapper<ServiceRequest>()
                .eq("applicant_id", AuthContext.get().userId())
                .eq("deleted", 0)
                .orderByDesc("created_at");
        applyStatusFilter(query, status);
        Page<ServiceRequest> result = requestMapper.selectPage(Page.of(page, size), query);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public RequestDetail detail(Long id) {
        ServiceRequest request = visibleRequest(id);
        ServiceItem item = itemMapper.selectById(request.getItemId());
        User applicant = userMapper.selectById(request.getApplicantId());
        if (item == null || applicant == null) {
            throw new BusinessException(404, "申请关联数据不存在");
        }
        Venue venue = request.getVenueId() == null ? null : venueMapper.selectById(request.getVenueId());
        List<ProcessRecord> records = recordMapper.selectList(new QueryWrapper<ProcessRecord>()
                .eq("request_id", id)
                .orderByAsc("created_at", "id"));
        Feedback feedback = feedbackMapper.selectOne(new QueryWrapper<Feedback>().eq("request_id", id));
        return new RequestDetail(request, item, toApplicantView(applicant), venue, records, feedback);
    }

    @Transactional
    public void cancel(Long id) {
        ServiceRequest request = ownRequest(id);
        change(request, RequestStatus.CANCELLED, "取消申请", "学生主动取消");
    }

    @Transactional
    public void feedback(Long id, FeedbackCommand command) {
        if (command == null) {
            throw new BusinessException("评价参数不能为空");
        }
        RequestRules.validateFeedback(command.score(), command.content());
        ServiceRequest request = ownRequest(id);
        RequestRules.requireTransition(request.getStatus(), RequestStatus.EVALUATED);
        if (feedbackMapper.selectCount(new QueryWrapper<Feedback>().eq("request_id", id)) > 0) {
            throw new BusinessException(409, "该申请已经评价");
        }

        Feedback feedback = new Feedback();
        feedback.setRequestId(id);
        feedback.setUserId(AuthContext.get().userId());
        feedback.setScore(command.score());
        feedback.setContent(blankToNull(command.content()));
        feedback.setCreatedAt(LocalDateTime.now());
        try {
            feedbackMapper.insert(feedback);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "该申请已经评价");
        }
        change(request, RequestStatus.EVALUATED, "评价服务", feedback.getContent());
    }

    public CertificateFile certificate(Long id) {
        ServiceRequest request = ownRequest(id);
        if (!List.of(RequestStatus.FINISHED.name(), RequestStatus.EVALUATED.name()).contains(request.getStatus())) {
            throw new BusinessException("申请办结后才能下载证明");
        }
        ServiceItem item = itemMapper.selectById(request.getItemId());
        if (item == null || !"CERTIFICATE".equals(item.getType())) {
            throw new BusinessException("该申请不是证明办理事项");
        }
        if (request.getCertificateNo() == null || request.getVerificationCode() == null) {
            throw new BusinessException(409, "证明尚未生成");
        }
        User applicant = userMapper.selectById(request.getApplicantId());
        if (applicant == null) {
            throw new BusinessException(404, "申请人不存在");
        }
        return new CertificateFile("在读证明-" + request.getCertificateNo() + ".html",
                renderCertificate(request, applicant));
    }

    public PageResult<RequestListItem> todo(String status, long page, long size) {
        RequestRules.validatePage(page, size);
        Long departmentId = managementDepartment();
        QueryWrapper<ServiceRequest> query = scopedQuery(departmentId).orderByDesc("created_at");
        applyStatusFilter(query, status);
        Page<ServiceRequest> result = requestMapper.selectPage(Page.of(page, size), query);
        List<ServiceRequest> requests = result.getRecords();
        if (requests.isEmpty()) {
            return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), List.of());
        }
        Map<Long, User> applicants = userMapper.selectByIds(requests.stream()
                        .map(ServiceRequest::getApplicantId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, ServiceItem> items = itemMapper.selectByIds(requests.stream()
                        .map(ServiceRequest::getItemId).distinct().toList()).stream()
                .collect(Collectors.toMap(ServiceItem::getId, Function.identity()));
        List<RequestListItem> records = requests.stream().map(request -> {
            User applicant = applicants.get(request.getApplicantId());
            ServiceItem item = items.get(request.getItemId());
            return new RequestListItem(request.getId(), request.getRequestNo(), request.getTitle(), request.getStatus(),
                    request.getCreatedAt(), applicant == null ? null : applicant.getRealName(),
                    item == null ? null : item.getName(), item == null ? null : item.getType());
        }).toList();
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Transactional
    public void accept(Long id) {
        ServiceRequest request = manageableRequest(id);
        request.setHandlerId(AuthContext.get().userId());
        request.setAcceptedAt(LocalDateTime.now());
        change(request, RequestStatus.ACCEPTED, "受理申请", "部门已受理");
    }

    @Transactional
    public void approve(Long id, ApproveCommand command) {
        if (command == null) {
            throw new BusinessException("审核参数不能为空");
        }
        RequestRules.validateApproval(command.approved(), command.comment());
        ServiceRequest request = manageableRequest(id);
        request.setHandlerId(AuthContext.get().userId());
        RequestStatus target = command.approved() ? RequestStatus.PROCESSING : RequestStatus.REJECTED;
        change(request, target, command.approved() ? "审核通过" : "审核驳回", RequestRules.clean(command.comment()));
    }

    @Transactional
    public void finish(Long id, FinishCommand command) {
        if (command == null) {
            throw new BusinessException("办结参数不能为空");
        }
        RequestRules.validateFinish(command.result());
        ServiceRequest request = manageableRequest(id);
        RequestRules.requireTransition(request.getStatus(), RequestStatus.FINISHED);
        ServiceItem item = itemMapper.selectById(request.getItemId());
        if (item == null) {
            throw new BusinessException(404, "服务事项不存在");
        }
        request.setResult(RequestRules.clean(command.result()));
        request.setFinishedAt(LocalDateTime.now());
        if ("CERTIFICATE".equals(item.getType())) {
            request.setCertificateNo(newIdentifier("CERT-", 20));
            request.setVerificationCode(newIdentifier("", 32));
        }
        change(request, RequestStatus.FINISHED, "办结服务", request.getResult());
    }

    public Stats stats() {
        Long departmentId = managementDepartment();
        long total = requestMapper.selectCount(scopedQuery(departmentId));
        long submitted = requestMapper.selectCount(scopedQuery(departmentId).eq("status", RequestStatus.SUBMITTED.name()));
        long processing = requestMapper.selectCount(scopedQuery(departmentId)
                .in("status", List.of(RequestStatus.ACCEPTED.name(), RequestStatus.PROCESSING.name())));
        long finished = requestMapper.selectCount(scopedQuery(departmentId)
                .in("status", List.of(RequestStatus.FINISHED.name(), RequestStatus.EVALUATED.name())));
        long rejected = requestMapper.selectCount(scopedQuery(departmentId).eq("status", RequestStatus.REJECTED.name()));
        return new Stats(total, submitted, processing, finished, rejected);
    }

    private void prepareRepair(ServiceRequest request, CreateRequestCommand command) {
        request.setLocation(RequestRules.clean(command.location()));
        request.setRepairCategory(RequestRules.clean(command.repairCategory()));
        request.setUrgency(RequestRules.clean(command.urgency()));
        RequestRules.validateRepair(request);
    }

    private void prepareCertificate(ServiceRequest request, CreateRequestCommand command) {
        request.setCertificateType(RequestRules.clean(command.certificateType()));
        request.setPurpose(RequestRules.clean(command.purpose()));
        request.setLanguage(RequestRules.clean(command.language()));
        request.setCopies(command.copies());
        request.setDeliveryMethod(RequestRules.clean(command.deliveryMethod()));
        RequestRules.validateCertificate(request);
    }

    private void prepareVenue(ServiceRequest request, CreateRequestCommand command, LocalDateTime now) {
        if (command.venueId() == null) {
            throw new BusinessException("场地申请必须选择场地");
        }
        Venue venue = venueMapper.selectByIdForUpdate(command.venueId());
        request.setVenueId(command.venueId());
        request.setEventName(RequestRules.clean(command.eventName()));
        request.setAppointmentStart(command.appointmentStart());
        request.setAppointmentEnd(command.appointmentEnd());
        request.setAttendeeCount(command.attendeeCount());
        request.setContactName(RequestRules.clean(command.contactName()));
        request.setContactPhone(RequestRules.clean(command.contactPhone()));
        RequestRules.validateVenue(request, venue, now);
        Long conflictId = requestMapper.findOverlappingVenueRequestForUpdate(
                command.venueId(), command.appointmentStart(), command.appointmentEnd());
        if (conflictId != null) {
            throw new BusinessException(409, "所选场地时段已被占用");
        }
    }

    private ServiceRequest ownRequest(Long id) {
        ServiceRequest request = requestMapper.selectById(id);
        if (request == null || Integer.valueOf(1).equals(request.getDeleted())
                || !request.getApplicantId().equals(AuthContext.get().userId())) {
            throw new BusinessException(404, "申请不存在");
        }
        return request;
    }

    private ServiceRequest visibleRequest(Long id) {
        ServiceRequest request = requestMapper.selectById(id);
        if (request == null || Integer.valueOf(1).equals(request.getDeleted())) {
            throw new BusinessException(404, "申请不存在");
        }
        AuthContext.AuthUser auth = AuthContext.get();
        if (auth.hasRole("ADMIN")) {
            return request;
        }
        if (auth.hasRole("STAFF")) {
            Long departmentId = staffDepartment(auth.userId());
            if (request.getDepartmentId().equals(departmentId)) {
                return request;
            }
            throw new BusinessException(403, "无权限查看其他部门申请");
        }
        if (auth.hasRole("STUDENT") && request.getApplicantId().equals(auth.userId())) {
            return request;
        }
        throw new BusinessException(403, "无权限查看该申请");
    }

    private ServiceRequest manageableRequest(Long id) {
        AuthContext.AuthUser auth = AuthContext.get();
        if (!auth.hasRole("ADMIN") && !auth.hasRole("STAFF")) {
            throw new BusinessException(403, "无权限处理申请");
        }
        return visibleRequest(id);
    }

    private Long managementDepartment() {
        AuthContext.AuthUser auth = AuthContext.get();
        if (auth.hasRole("ADMIN")) {
            return null;
        }
        if (!auth.hasRole("STAFF")) {
            throw new BusinessException(403, "无权限查看处理数据");
        }
        return staffDepartment(auth.userId());
    }

    private Long staffDepartment(Long userId) {
        User staff = userMapper.selectById(userId);
        if (staff == null || staff.getDepartmentId() == null) {
            throw new BusinessException(403, "部门人员未绑定部门");
        }
        return staff.getDepartmentId();
    }

    private void change(ServiceRequest request, RequestStatus target, String action, String comment) {
        RequestRules.requireTransition(request.getStatus(), target);
        String from = request.getStatus();
        request.setStatus(target.name());
        request.setUpdatedAt(LocalDateTime.now());
        if (requestMapper.updateById(request) != 1) {
            throw new BusinessException(409, "申请已被其他操作更新，请刷新后重试");
        }
        record(request, from, target.name(), action, comment);
        noticeService.create(request.getApplicantId(), request.getId(), action,
                "申请单 " + request.getRequestNo() + " 状态变更为 " + target.name());
        logService.log("REQUEST", action, request.getRequestNo());
    }

    private void record(ServiceRequest request, String from, String to, String action, String comment) {
        ProcessRecord record = new ProcessRecord();
        record.setRequestId(request.getId());
        record.setOperatorId(AuthContext.get().userId());
        record.setFromStatus(from);
        record.setToStatus(to);
        record.setAction(action);
        record.setComment(comment);
        record.setCreatedAt(LocalDateTime.now());
        recordMapper.insert(record);
    }

    private QueryWrapper<ServiceRequest> scopedQuery(Long departmentId) {
        QueryWrapper<ServiceRequest> query = new QueryWrapper<ServiceRequest>().eq("deleted", 0);
        if (departmentId != null) {
            query.eq("department_id", departmentId);
        }
        return query;
    }

    private void applyStatusFilter(QueryWrapper<ServiceRequest> query, String status) {
        if (status == null || status.isBlank()) {
            return;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        try {
            RequestStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("申请状态不正确");
        }
        query.eq("status", normalized);
    }

    private ApplicantView toApplicantView(User user) {
        return new ApplicantView(user.getId(), user.getUsername(), user.getRealName(), user.getStudentNo(),
                user.getPhone(), user.getEmail(), user.getCollege(), user.getMajor(), user.getDepartmentId());
    }

    private String renderCertificate(ServiceRequest request, User applicant) {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>在读证明</title>
                  <style>
                    body { margin: 0; background: #f4f5f7; color: #202124; font-family: "Microsoft YaHei", sans-serif; }
                    .certificate { max-width: 760px; margin: 32px auto; padding: 56px 64px; background: #fff; border: 1px solid #c8ccd0; box-sizing: border-box; }
                    h1 { margin: 0 0 40px; text-align: center; font-size: 28px; letter-spacing: 0; }
                    p { margin: 16px 0; font-size: 16px; line-height: 2; }
                    .meta { margin-top: 44px; font-size: 14px; color: #4b5563; }
                    .signature { margin-top: 56px; text-align: right; }
                    @media print { body { background: #fff; } .certificate { margin: 0 auto; border: 0; } }
                  </style>
                </head>
                <body>
                  <main class="certificate">
                    <h1>在读证明</h1>
                    <p>兹证明 <strong>%s</strong>，学号 <strong>%s</strong>，现就读于 <strong>%s</strong> <strong>%s</strong> 专业。</p>
                    <p>证明类型：%s；用途：%s；语言：%s；申请份数：%s。</p>
                    <p>该生当前为我校在读学生，特此证明。</p>
                    <div class="meta">
                      <p>证明编号：%s</p>
                      <p>校验码：%s</p>
                    </div>
                    <div class="signature">
                      <p>学生事务中心</p>
                      <p>%s</p>
                    </div>
                  </main>
                </body>
                </html>
                """.formatted(
                html(applicant.getRealName()), html(applicant.getStudentNo()), html(applicant.getCollege()),
                html(applicant.getMajor()), html(request.getCertificateType()), html(request.getPurpose()),
                html(request.getLanguage()), request.getCopies(), html(request.getCertificateNo()),
                html(request.getVerificationCode()), request.getFinishedAt().format(CERTIFICATE_TIME));
    }

    private static String newIdentifier(String prefix, int length) {
        String value = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return prefix + value.substring(0, length);
    }

    private static String blankToNull(String value) {
        String cleaned = RequestRules.clean(value);
        return cleaned == null || cleaned.isEmpty() ? null : cleaned;
    }

    private static String html(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    public record CreateRequestCommand(
            Long itemId,
            String title,
            String content,
            String location,
            String repairCategory,
            String urgency,
            String certificateType,
            String purpose,
            String language,
            Integer copies,
            String deliveryMethod,
            Long venueId,
            String eventName,
            LocalDateTime appointmentStart,
            LocalDateTime appointmentEnd,
            Integer attendeeCount,
            String contactName,
            String contactPhone
    ) {
    }

    public record FeedbackCommand(Integer score, String content) {
    }

    public record ApproveCommand(Boolean approved, String comment) {
    }

    public record FinishCommand(String result) {
    }

    public record ApplicantView(Long id, String username, String realName, String studentNo, String phone,
                                String email, String college, String major, Long departmentId) {
    }

    public record RequestDetail(ServiceRequest request, ServiceItem item, ApplicantView applicant, Venue venue,
                                List<ProcessRecord> records, Feedback feedback) {
    }

    public record RequestListItem(Long id, String requestNo, String title, String status, LocalDateTime createdAt,
                                  String applicantName, String itemName, String itemType) {
    }

    public record CertificateFile(String filename, String content) {
    }

    public record Stats(long total, long submitted, long processing, long finished, long rejected) {
    }
}
