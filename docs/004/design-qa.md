# 管理端活动审查台 Design QA

- source visual truth path: `output/playwright/admin-audit/reference-activity-review.png`
- implementation screenshot path: `output/playwright/admin-audit/04-activity-detail-redesign.png`
- combined comparison evidence: `output/playwright/admin-audit/05-design-comparison.png`
- viewport: `1440 x 1024`
- state: 平台管理员已登录，活动审核队列存在一条待审活动，打开活动详情，任务尚未认领
- primary interactions tested: 登录、工作台加载、进入活动审核、打开活动详情、加载素材/申请/参与者/审核记录、关闭和认领入口
- console errors checked: 0

## Full-view comparison

参考设计与实现已在同一张 `05-design-comparison.png` 中按 1440px 宽并排比较。实现保留了深墨绿主导航、左侧案件队列、中部证据与事实区域、右侧摘要与吸底决策区，主要区域比例、信息密度、6px 圆角和低饱和语义色与参考方向一致。

## Focused comparison

未再制作局部裁剪。并排对照图保持每侧 1440px 原始宽度，标题、队列元数据、证据页签、事实网格和决策控件均可直接辨认，局部裁剪不会增加判断信息。

## Required fidelity surfaces

- Fonts and typography: 使用中文系统无衬线字体、正常字距和 600/700 层级；标题、正文、表格与辅助文本未出现截断或模糊缩放。
- Spacing and layout rhythm: 三栏网格、分隔线、固定决策区与参考一致；实现保留更大的证据阅读宽度，适配真实长文本。
- Colors and visual tokens: 墨绿导航、暖灰画布、白色证据面、绿色通过和橙红驳回均由全局 token 统一。
- Image quality and asset fidelity: 当前真实活动没有关联媒体，因此显示空证据态，没有使用占位海报伪造审核材料；有媒体时通过私有文件 URL 打开原文件。
- Copy and content: 所有机器枚举已中文化，页面只展示真实接口返回的活动、申请、参与者和审计内容。

## Comparison history

1. 首轮发现 P1：详情仅为普通右侧抽屉，缺少参考设计中的案件队列，且在 1440px 视口中无法形成完整审查工作区。
2. 修复：抽屉改为从主导航右侧开始的全高三栏审查台，新增真实队列切换，并让中部证据和右侧决策区独立滚动。
3. 次轮发现 P2：抽屉未 teleport 到 body，实际高度跟随页面内容，只显示约三分之一视口。
4. 修复：启用 `append-to-body`，保持无半透明遮罩，决策按钮固定在视口底部。
5. 复核证据：`04-activity-detail-redesign.png` 与 `05-design-comparison.png`，无剩余 P0/P1/P2。

## Findings

- [P3] 参考设计含海报、多人申请和完整审计记录，当前本地活动缺少这些真实数据。实现已提供对应页签、空态和私有素材预览，不使用假数据填充。
- [P3] 参考设计将案件标题放在最上方横向标题栏，实现将标题放在证据主栏，以便案件切换时保持队列和操作路径稳定。

## Implementation checklist

- [x] 三栏活动审查工作区
- [x] 真实活动详情、发起人、申请、参与者、媒体和时间线接口
- [x] 认领、通过、驳回与后端状态校验
- [x] 加载、空数据、错误重试和无权限状态
- [x] 1440 x 1024 浏览器交互与 console 检查

final result: passed
