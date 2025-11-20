package com.almang.inventory.admin.controller;

import com.almang.inventory.admin.dto.request.CreateStoreRequest;
import com.almang.inventory.admin.dto.response.CreateStoreResponse;
import com.almang.inventory.admin.service.AdminService;
import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.SuccessMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "내부 API")
public class AdminController {

    private final AdminService adminService;

    @PostMapping
    @Operation(summary = "상점 생성", description = "상점을 생성하여 상점 정보를 반환합니다.")
    public ResponseEntity<ApiResponse<CreateStoreResponse>> createStore(
            @Valid @RequestBody CreateStoreRequest request
    ) {
        log.info("[AdminController] 상점 생성 요청 - name: {}", request.name());
        CreateStoreResponse response = adminService.createStore(request);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessMessage.CREATE_STORE_SUCCESS.getMessage(), response)
        );
    }
}
