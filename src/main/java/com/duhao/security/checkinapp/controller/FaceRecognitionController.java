package com.duhao.security.checkinapp.controller;

import com.duhao.security.checkinapp.service.FaceRecognitionService;
import com.duhao.security.checkinapp.dto.FaceRecognitionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class FaceRecognitionController {

    private final FaceRecognitionService faceRecognitionService;

    @Autowired
    public FaceRecognitionController(FaceRecognitionService faceRecognitionService) {
        this.faceRecognitionService = faceRecognitionService;
    }

    @PostMapping(path = "/face_recognition", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FaceRecognitionResponse> recognize(
            @RequestParam("faceImage") MultipartFile file, @RequestParam("employeeId")  String employeeId) {

        try {
            boolean recognize = faceRecognitionService.verify(file, employeeId);
            if (recognize) {
                return ResponseEntity.ok().body(FaceRecognitionResponse.ok());
            } else {
                return ResponseEntity
                        .badRequest()
                        .body(FaceRecognitionResponse.fail("活体或人脸识别未通过"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(FaceRecognitionResponse.fail("识别服务异常"));
        }
    }
}
