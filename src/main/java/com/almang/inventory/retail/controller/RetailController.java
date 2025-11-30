package com.almang.inventory.retail.controller;

import com.almang.inventory.retail.service.RetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Retail", description = "소매 판매 관리 API")
@RestController
@RequestMapping("/api/v1/retail")
@RequiredArgsConstructor
public class RetailController {

    private final RetailService retailService;

    @Operation(summary = "엑셀 파일 업로드", description = "엑셀 파일을 업로드하여 소매 판매 내역을 등록하고 재고를 차감합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRetailExcel(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please select a file to upload"));
        }

        try {
            retailService.processRetailExcel(file);
            return ResponseEntity.ok(Map.of("success", true, "message", "Retail data processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
