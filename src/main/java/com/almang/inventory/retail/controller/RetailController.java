package com.almang.inventory.retail.controller;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.retail.dto.response.RetailResponse;
import com.almang.inventory.retail.service.RetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Tag(name = "Retail", description = "소매 판매 관리 API")
@RestController
@RequestMapping("/api/v1/retail")
@RequiredArgsConstructor
public class RetailController {

    private final RetailService retailService;

    @Operation(summary = "엑셀 파일 업로드", description = "엑셀 파일을 업로드하여 소매 판매 내역을 등록하고 재고를 차감합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRetailExcel(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestPart("file") MultipartFile file) {
        Long userId = userPrincipal.getId();
        log.info("[RetailController] 엑셀 파일 업로드 요청 - userId: {}, filename: {}", userId, file.getOriginalFilename());
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a file to upload"));
        }

        try {
            RetailService.RetailUploadResult result = retailService.processRetailExcel(file);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Retail data processed successfully");
            response.put("processedCount", result.processedCount());
            response.put("skippedProducts", result.skippedProducts());
            response.put("skippedCount", result.skippedProducts().size());
            
            if (!result.skippedProducts().isEmpty()) {
                response.put("warning", String.format("%d개의 상품이 스킵되었습니다.", result.skippedProducts().size()));
            }
            
            log.info("[RetailController] 엑셀 파일 업로드 성공 - userId: {}, processedCount: {}, skippedCount: {}", 
                    userId, result.processedCount(), result.skippedProducts().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[RetailController] 엑셀 파일 업로드 실패 - userId: {}, error: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "소매 내역 목록 조회", description = "소매 내역을 페이지네이션, 날짜 검색 조건과 함께 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<RetailResponse>>> getRetailList(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "soldDate", required = false) LocalDate soldDate,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate
    ) {
        Long userId = userPrincipal.getId();
        log.info("[RetailController] 소매 내역 목록 조회 요청 - userId: {}, page: {}, size: {}, soldDate: {}, startDate: {}, endDate: {}",
                userId, page, size, soldDate, startDate, endDate);
        PageResponse<RetailResponse> response =
                retailService.getRetailList(userId, page, size, soldDate, startDate, endDate);

        return ResponseEntity.ok(
                ApiResponse.success("소매 내역 목록 조회 성공", response)
        );
    }

    @GetMapping("/date")
    @Operation(summary = "특정 날짜 소매 내역 조회", description = "특정 날짜의 소매 내역을 조회합니다.")
    public ResponseEntity<ApiResponse<List<RetailResponse>>> getRetailListByDate(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @RequestParam(value = "soldDate", required = true) LocalDate soldDate
    ) {
        Long userId = userPrincipal.getId();
        log.info("[RetailController] 특정 날짜 소매 내역 조회 요청 - userId: {}, soldDate: {}", userId, soldDate);
        List<RetailResponse> response = retailService.getRetailListByDate(userId, soldDate);

        return ResponseEntity.ok(
                ApiResponse.success("특정 날짜 소매 내역 조회 성공", response)
        );
    }
}
