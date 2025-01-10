package kr.inuappcenterportal.inuportal.global.service;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class ImageService {

    @Value("${bookImagePath}")
    private String bookImagePath;

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

    public byte[] getThumbnail(Long bookId){
        String fileName = bookId+"-1";
        Path filePath = Paths.get(bookImagePath, fileName);
        try {
            return Files.readAllBytes(filePath);
        }
        catch (Exception e){
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
    }

    public void saveBookImage(Long id, List<MultipartFile> images) throws IOException {
        for (int i = 1; i < images.size() + 1; i++) {
            MultipartFile file = images.get(i - 1);
            String fileName = id + "-" + i;
            Path filePath = Paths.get(bookImagePath, fileName);
            Files.write(filePath, file.getBytes());
        }
    }

    public List<byte[]> getImages(Long id) {
        List<byte[]> images = new ArrayList<>();
        try (Stream<Path> paths = Files.list(Paths.get(bookImagePath))) {
            for (Path filePath : paths.filter(filePath -> filePath.getFileName().toString().startsWith(id + "-"))
                    .sorted()
                    .toList()) {
                images.add(Files.readAllBytes(filePath));
            }
        } catch (IOException e) {
            throw new MyException(MyErrorCode.IMAGE_NOT_FOUND);
        }
        return images;
    }




}
