package com.duhao.security.checkinapp.service;

import org.springframework.web.multipart.MultipartFile;

public interface FaceRecognitionService {
    /**
     * 对上传的照片做活体 + 人脸识别
     * @param image 活体检测截图
     * @param employeeId 员工 ID，用于在 Redis 查 embedding
     * @return true = 活体+人脸识别通过
     * @throws Exception 识别异常
     */
    boolean verify(MultipartFile image, String employeeId) throws Exception;
}
