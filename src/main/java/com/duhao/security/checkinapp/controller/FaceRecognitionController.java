package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.service.FaceRecognitionService;
import com.duhao.security.checkinapp.dto.FaceRecognitionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class FaceRecognitionController {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionController.class);
    private final FaceRecognitionService faceRecognitionService;

    @Autowired
    public FaceRecognitionController(FaceRecognitionService faceRecognitionService) {
        this.faceRecognitionService = faceRecognitionService;
    }

    @PostMapping(path = "/face_recognition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FaceRecognitionResponse> recognize(
            @RequestParam("faceImage") MultipartFile file, @RequestParam("employeeId")  String employeeId) {

        logger.info("🔍 人脸识别请求 - 员工ID: {}", employeeId);
        logger.debug("├─ 文件名: {}", file.getOriginalFilename());
        logger.debug("├─ 文件大小: {} bytes", file.getSize());
        logger.debug("└─ 文件类型: {}", file.getContentType());

        try {
            long startTime = System.currentTimeMillis();
            boolean recognize = faceRecognitionService.verify(file, employeeId);
            long duration = System.currentTimeMillis() - startTime;
            
            if (recognize) {
                logger.info("✅ 人脸识别成功 - 员工ID: {}, 耗时: {}ms", employeeId, duration);
                return ResponseEntity.ok().body(FaceRecognitionResponse.ok());
            } else {
                logger.warn("❌ 人脸识别失败 - 员工ID: {}, 耗时: {}ms", employeeId, duration);
                return ResponseEntity
                        .badRequest()
                        .body(FaceRecognitionResponse.fail("活体或人脸识别未通过"));
            }
        } catch (Exception e) {
            logger.error("💥 人脸识别服务异常 - 员工ID: {}, 错误: {}", employeeId, e.getMessage(), e);
            return ResponseEntity
                    .internalServerError()
                    .body(FaceRecognitionResponse.fail("识别服务异常"));
        }
    }
}
