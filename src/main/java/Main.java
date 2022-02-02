import net.sourceforge.tess4j.TesseractException;

import java.io.File;

public class Main {
    public static void main(String[] args) throws TesseractException {
        //Путь к файлу
        File imagePath = new File("img\\1.jpg");
        //Отобразить распознаный текст
        System.out.println(TextRecognizer.getRecognizedText(imagePath));
    }
}
