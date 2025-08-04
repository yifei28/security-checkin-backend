package com.duhao.security.checkinapp.impl;

import com.duhao.security.checkinapp.service.FaceRecognitionService;
import com.duhao.security.checkinapp.dto.RecognizeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${face.service.url}")
    private String faceServiceUrl;

    @Override
    public boolean verify(MultipartFile image, String employeeId) throws Exception {
        // 1. 构建 multipart/form-data 请求体，包含 faceImage 和 employeeID
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("faceImage", new MultipartInputStreamFileResource(
                image.getInputStream(), image.getOriginalFilename(), image.getSize()));
        body.add("employeeId", employeeId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        // 2. 转发给 Python 服务
        ResponseEntity<RecognizeResult> response = restTemplate
                .exchange(faceServiceUrl, HttpMethod.POST, requestEntity, RecognizeResult.class);

        // 3. 检查响应
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("识别服务调用失败, HTTP " + response.getStatusCode());
        }
        RecognizeResult result = response.getBody();

        // 4. 返回注册／验证结果
        return result.isSuccess();
    }

    /** 将 MultipartFile 转成 Resource，保证 RestTemplate 能正确上传 */
    private static class MultipartInputStreamFileResource extends InputStreamResource {
        private final String filename;
        private final long size;
        MultipartInputStreamFileResource(InputStream inputStream, String filename, long size) {
            super(inputStream);
            this.filename = filename;
            this.size = size;
        }
        @Override public String getFilename() { return this.filename; }
        @Override public long contentLength() { return size; }
    }
}
