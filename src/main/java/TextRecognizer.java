import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class TextRecognizer {

    public static String getRecognizedText(File imagePath) throws TesseractException {
        // Подключаем openCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Загружаем изображение в матрицу OpenCV
        Mat img = Imgcodecs.imread(imagePath.getPath());

        //Исходное изображение
        //imageShow(img);

        // Используем фильтр
        Mat result = filteringImage(img);

        //imageShow(result);

        // Распознаем текст
        return textCleaner(getText(matToBufferedImage(result)));

    }


    //Накладываем фильтр повышая четкость
    private static Mat filteringImage(Mat img) {
        //Пустая матрица
        Mat result = new Mat(img.size(), img.type());
        // Конвертируем изображение в другое цветовое пространство
        Imgproc.cvtColor(img, img, Imgproc.COLOR_Luv2LRGB);
        // Размытие
        Imgproc.bilateralFilter(img, result, 100, 160, 120);

        return result;

    }

    //Чернобелое изображение
    private static Mat filteringImageBlackAndWhite(Mat img) {
        Mat result = new Mat(img.size(), img.type());
        Imgproc.cvtColor(img, result, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(result, img, 128, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        //Imgproc.bilateralFilter(img, result, 100, 160, 120);
        Mat kernel = new Mat(1, 1, CvType.CV_8UC1, new Scalar(1.0));
        Imgproc.erode(img, result, kernel);
        Imgproc.threshold(result, img, 128, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Imgproc.medianBlur(img, result, 1);
        return result;
    }

    //Просто набор методов которые будут полезны в дальнейщем
    private static void methods(Mat img) {
        // Тест обработки изображения
        // Сюда записываем изображение создаем пустое изображение
        Mat imgEmpty = new Mat(img.size(), img.type());

        // Накладываем фильтры ядром мы проходимся по тексту
        Mat kernel = new Mat(5, 5, CvType.CV_8UC1, new Scalar(1.0));

        // Применяем различные функции

        // Сужаем темные зоры расширяем светлые
        // img - к чему применяем
        // imgEmpty - куда записываем
        Imgproc.dilate(img, imgEmpty, kernel);

        // Расширяем темные сужаем светлые
        Imgproc.erode(img, imgEmpty, kernel);

        // Конвертируем изображение в другое цветовое пространство
        Imgproc.cvtColor(img, imgEmpty, Imgproc.COLOR_BGR2GRAY);

        // Блюр изображения
        Imgproc.GaussianBlur(img, imgEmpty, new Size(15, 15), 0);

        // Получаем границы изображения
        Imgproc.Canny(img, imgEmpty, 2, 2);

        // Меняем размер
        Imgproc.resize(img, imgEmpty, new Size(200, 200));

        // Обрезаем изображение обрезаем по
        // img.rowRange(100,250) - Y
        // colRange(50,250) - X
        Mat imgCrop = img.rowRange(100, 250).colRange(50, 250);

        // Можно обработать часть изображения
        // берем с исхожного изображения часть которую будем обрабатывать
        // берем изображение на которое будеп применять
        Imgproc.erode(img.rowRange(100, 250).colRange(50, 250), img.rowRange(100, 250).colRange(50, 250), kernel);

        //Сохраняем изображение
        Imgcodecs.imwrite("img\\image.jpg", img);

        imageShow(img);
        imageShow(imgEmpty);
        imageShow(imgCrop);
        // Конец теста
    }

    //Преобразование в буфферед image
    private static BufferedImage matToBufferedImage(Mat m) {
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

    //Показать изображение
    private static void imageShow(Mat img) {
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

    //Получение текста с изображения
    private static String getText(BufferedImage image) throws TesseractException {
        // Подключаем файлы данных tesseract
        String tessdata = "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata";
        //Создаем объект который будет читать данные
        Tesseract tesseract = new Tesseract();
        //Подключем данные
        tesseract.setDatapath(tessdata);
        //Сообщаем шрифты
        tesseract.setOcrEngineMode(3);
        tesseract.setPageSegMode(3);
        tesseract.setLanguage("rus+eng");
        //Распознавание данных этот метод работает либо с файлом либо с imageBuffer
        String text = tesseract.doOCR(image);
        return text;
    }

    //Метод очистки текста
    private static String textCleaner(String text) {
        //Удаление пустых строк, и символов кроме алфавиных, и слов длинны <=3
        String adjusted = text.
                replaceAll("[^A-Za-zА-Яа-я0-9\\. \\n]", "").
                replaceAll("(?m)^.{0,3}$", "").
                replaceAll("(?m)^[ \t]*\r?\n", "").toUpperCase(Locale.ROOT);

        //Удалять спец символы не похожие на цифры и буквы
        //Если в сторке меньше 3 идуших друг за другом симоволов или они разделены пробелами удалять их
        //ВОДИТЕЛЬСНПЕ УДОСТОВЕРЕНИЕ //Без ошибок и в верхнем регистре
        //петРОВ // Первая буква с заглавной
        //PETROV // Первую букву с заглавной
        //НИКИТА ВЛАДИМИРОВИЧ //Первую букву с заглавной втрое слово с заглавной
        //NIKITA VLADIMIRGVICH  //Первую букву с заглавной втрое слово с заглавной
        //06.01.1991 //приводить к дате в любом случае
        //БУРЯТСКОЙ РЕСП.  //Первую букву с заглавной втрое слово с заглавной
        //BURYATSKOY RESP, //Первую букву с заглавной втрое слово с заглавной
        //43.04.2013 4b) 13.04.2023 // отсавить только цифры похожие на дату приводить к заглавной
        //ГИБДД 2301 // Не изменять
        //GIBDD 2301 // Не изменять
        //22 12 2342112 // убрать пробелы, проверять длинну
        //ЧУВАШСКАЯ РЕСП. //Первую букву с заглавной втрое слово с заглавной
        //BURYATSKAY RESP. // //Первую букву с заглавной втрое слово с заглавной
        return adjusted;
    }
}
