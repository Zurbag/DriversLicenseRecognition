import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

public class Main {
    public static void main(String[] args) throws TesseractException {
        // Подключаем openCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        //Путь к файлу
        File imagePath = new File("img\\cd8.jpg");

        // Загружаем изображение в матрицу OpenCV
        Mat img = Imgcodecs.imread(imagePath.getPath());
        BufferedImage bufferedImage = matToBufferedImage(img);
        System.out.println(getText(bufferedImage));

    }

    //Преобразование в буфферед image
    public static BufferedImage matToBufferedImage(Mat m) {
        if (m == null || m.empty()) return null;
        if (m.depth() == CvType.CV_8U) {
        } else if (m.depth() == CvType.CV_16U) { // CV_16U => CV_8U
            Mat m_16 = new Mat();
            m.convertTo(m_16, CvType.CV_8U, 255.0 / 65535);
            m = m_16;
        } else if (m.depth() == CvType.CV_32F) { // CV_32F => CV_8U
            Mat m_32 = new Mat();
            m.convertTo(m_32, CvType.CV_8U, 255);
            m = m_32;
        } else
            return null;
        int type = 0;
        if (m.channels() == 1)
            type = BufferedImage.TYPE_BYTE_GRAY;
        else if (m.channels() == 3)
            type = BufferedImage.TYPE_3BYTE_BGR;
        else if (m.channels() == 4)
            type = BufferedImage.TYPE_4BYTE_ABGR;
        else
            return null;
        byte[] buf = new byte[m.channels() * m.cols() * m.rows()];
        m.get(0, 0, buf);
        byte tmp = 0;
        if (m.channels() == 4) { // BGRA => ABGR
            for (int i = 0; i < buf.length; i += 4) {
                tmp = buf[i + 3];
                buf[i + 3] = buf[i + 2];
                buf[i + 2] = buf[i + 1];
                buf[i + 1] = buf[i];
                buf[i] = tmp;
            }
        }
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        byte[] data =
                ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buf, 0, data, 0, buf.length);
        return image;
    }
    public static void imageShow(Mat img) {
        // Просмотр изображения
        JFrame window = new JFrame("Window:");
        // Создаем контейнер для изображения
        JLabel screen = new JLabel();
        // Устанавливаем операцию закрытия по умолчанию
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Делаем окно отображения видимым
        window.setVisible(true);

        /* Преобразуем изображение в матрицу байтов с целью
           получить массив байтов (пикселей). */
        MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(".png", img, buf);

        /* Преобразуем массив пикселей в ImageIcon,
           изображение которое будет отображатся. */
        ImageIcon ic = new ImageIcon(buf.toArray());

        // Привязываем изображение к контейнеру.
        screen.setIcon(ic);
        // Привязываем контейнер к окну отображения.
        window.getContentPane().add(screen);
        window.pack();
    }
    public static String getText(BufferedImage image) throws TesseractException {
        // Подключаем файлы данных tesseract
        String tessdata = "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata";
        //Создаем объект который будет читать данные
        Tesseract tesseract = new Tesseract();
        //Подключем данные
        tesseract.setDatapath(tessdata);
        //Сообщаем шрифты
        tesseract.setLanguage("rus+eng");
        //Распознавание данных этот метод работает либо с файлом либо с imageBuffer
        String text = tesseract.doOCR(image);
        return text;
    }
}
