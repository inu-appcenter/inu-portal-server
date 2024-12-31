package kr.inuappcenterportal.inuportal.global.service;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ImageService {
    public void saveImage(Long id, List<MultipartFile> images,String path) throws IOException {
        for (int i = 1; i < images.size() + 1; i++) {
            MultipartFile file = images.get(i - 1);
            String fileName = id + "-" + i;
            Path filePath = Paths.get(path, fileName);
            Files.write(filePath, file.getBytes());
        }
    }

    public void updateImage(Long id,long presentImageCount, List<MultipartFile> images,String path) throws IOException {
        deleteAllImage(id,presentImageCount,path);
        saveImage(id, images, path);
    }

    public byte[] getImage(Long id, Long imageId, String path){
        String fileName = id+"-"+imageId;
        Path filePath = Paths.get(path, fileName);
        try {
            return Files.readAllBytes(filePath);
        }
        catch (Exception e){
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
    }

    public void deleteAllImage(Long id, Long imageCount,String path) throws IOException {
        for(int i = 1 ; i < imageCount ; i++){
            String fileName = id + "-" + i;
            Path filePath = Paths.get(path, fileName);
            Files.deleteIfExists(filePath);
        }
    }
}
