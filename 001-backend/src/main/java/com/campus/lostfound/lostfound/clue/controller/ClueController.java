package com.campus.lostfound.lostfound.clue.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.lostfound.clue.entity.LfClueFeedback;
import com.campus.lostfound.lostfound.clue.service.ClueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClueController {
    private final ClueService clueService;

    @PostMapping("/items/{id}/clues")
    public Result<LfClueFeedback> create(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(clueService.create(id, body));
    }

    @GetMapping("/users/me/clues")
    public Result<PageResult<LfClueFeedback>> mine(@RequestParam Map<String, String> query) {
        return Result.ok(clueService.mine(query));
    }

    @PostMapping("/clues/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        clueService.confirm(id, body);
        return Result.ok();
    }
}
