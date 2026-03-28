package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.service.OCRService;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;

@Service
public class OCRServiceImpl implements OCRService {

    @Override
    public String extractTextFromPdfUrl(String pdfUrl) {
        StringBuilder extractedText = new StringBuilder();

        try {
            InputStream in = new URL(pdfUrl).openStream();
            PDDocument document = PDDocument.load(in);
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
            tesseract.setLanguage("eng");

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                String text = tesseract.doOCR(bim);
                extractedText.append(text).append("\n\n");
            }

            document.close();
            in.close();

            // 1. Clean the text and save it to a variable
            String finalCleanedText = cleanOcrText(extractedText.toString());

            // 👇 2. PRINT TO CONSOLE 👇
            System.out.println("\n--- RAW OCR OUTPUT ---");
            System.out.println(finalCleanedText);
            System.out.println("----------------------\n");

            // 3. Return the text to the grading orchestrator
            return finalCleanedText;

        } catch (TesseractException e) {
            System.err.println("OCR Engine Failed to read the image: " + e.getMessage());
            return "ERROR: Could not read handwriting.";
        } catch (Exception e) {
            System.err.println("Failed to process the PDF: " + e.getMessage());
            return "ERROR: Could not process the PDF document.";
        }
    }

    /**
     * NEW: OCR Pre-processing Pipeline
     * Cleans up garbage characters, weird spacing, and formatting artifacts.
     */
    private String cleanOcrText(String rawText) {
        if (rawText == null) return "";

        return rawText
                // Remove non-printable ASCII characters (keeps standard text, numbers, punctuation, and newlines)
                .replaceAll("[^\\x20-\\x7e\\x0A\\x0D]", "")
                // Replace multiple spaces with a single space
                .replaceAll(" +", " ")
                // Replace 3 or more consecutive newlines with just 2
                .replaceAll("\\n{3,}", "\n\n")
                // Trim leading/trailing whitespace
                .trim();
    }
}