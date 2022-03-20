package io.chrislowe.discordle.util;

import com.google.common.annotations.VisibleForTesting;
import io.chrislowe.discordle.game.guess.LetterGuess;
import io.chrislowe.discordle.game.guess.LetterState;
import io.chrislowe.discordle.game.guess.WordGuess;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WordGraphicBuilder {
    private static final int BOX_SIZE = 48;
    private static final int BOX_CURVE = 4;
    private static final int BOX_PADDING_X = 4;
    private static final int BOX_PADDING_Y = 8;

    private static final String FONT_NAME = "DejaVu Sans";
    private static final int FONT_SIZE = 28;

    private static final Color BG_COLOR = new Color(222, 211, 211);
    private static final Color EMPTY_COLOR = new Color(0, 0, 0);
    private static final Color CORRECT_COLOR = new Color(0, 153, 0);
    private static final Color MISMATCH_COLOR = new Color(170, 170, 0);
    private static final Color MISSING_COLOR = new Color(64, 64, 64);
    private static final Color UNKNOWN_COLOR = new Color(128, 128, 128);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);

    private final List<WordGuess> wordGuesses;
    private final int wordSize;
    private final int maxWordGuesses;

    public WordGraphicBuilder(int wordSize, int maxWordGuesses) {
        this.wordGuesses = new ArrayList<>();
        this.wordSize = wordSize;
        this.maxWordGuesses = maxWordGuesses;
    }

    public WordGraphicBuilder addWordGuesses(Collection<WordGuess> wordGuesses) {
        this.wordGuesses.addAll(wordGuesses);
        return this;
    }

    public byte[] buildAsPng() {
        try {
            BufferedImage image = buildAsBufferedImage();
            var outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating word graphic", e);
        }
    }

    @VisibleForTesting
    BufferedImage buildAsBufferedImage() {
        BufferedImage image = new BufferedImage(getImageWidth(), getImageHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));
            FontMetrics fontMetrics = graphics.getFontMetrics();

            // Draw background
            graphics.setColor(BG_COLOR);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

            // Draw boxes and text
            int gridWidth = (BOX_PADDING_X * 2) + BOX_SIZE;
            int gridHeight = (BOX_PADDING_Y * 2) + BOX_SIZE;
            for (int row = 0; row < maxWordGuesses; row++) {
                WordGuess wordGuess = wordGuesses.size() > row ? wordGuesses.get(row) : null;
                // Left pad for keyboard, when |wordGuess| < wordSize 
                int leftpad = wordGuess == null
                    ? 0
                    : ((gridWidth * (wordSize - wordGuess.size())) + BOX_PADDING_X) / 2;
                for (int col = 0; col < wordSize; col++) {
                    // Short circuit for keyboard, when |wordGuess| < wordSize
                    if (wordGuess != null && col == wordGuess.size()) {
                        break;
                    }
                    int startX = (gridWidth * col) + BOX_PADDING_X + leftpad;
                    int startY = (gridHeight * row) + BOX_PADDING_Y;

                    LetterGuess letterGuess = wordGuess != null ? wordGuess.getLetterGuess(col) : null;

                    // Draw box
                    if (letterGuess == null) {
                        graphics.setColor(EMPTY_COLOR);
                        graphics.drawRoundRect(startX, startY, BOX_SIZE, BOX_SIZE, BOX_CURVE, BOX_CURVE);
                    } else {
                        if (letterGuess.state() == LetterState.CORRECT) {
                            graphics.setColor(CORRECT_COLOR);
                        } else if (letterGuess.state() == LetterState.MISMATCH) {
                            graphics.setColor(MISMATCH_COLOR);
                        } else if (letterGuess.state() == LetterState.MISSING) {
                            graphics.setColor(MISSING_COLOR);
                        } else {
                            graphics.setColor(UNKNOWN_COLOR);
                        }
                        graphics.fillRoundRect(startX, startY, BOX_SIZE, BOX_SIZE, BOX_CURVE, BOX_CURVE);
                    }

                    // Draw text
                    if (letterGuess != null) {
                        char letter = letterGuess.letter();
                        int textStartX = startX + ((BOX_SIZE - fontMetrics.charWidth(letter)) / 2);
                        int textStartY = startY + (int)((BOX_SIZE - fontMetrics.getHeight()) * 2.35);

                        graphics.setColor(TEXT_COLOR);
                        graphics.drawString(String.valueOf(letter), textStartX, textStartY);
                    }
                }
            }

            return image;
        } finally {
            graphics.dispose();
        }
    }

    private int getImageWidth() {
        return wordSize * ((BOX_PADDING_X * 2) + BOX_SIZE);
    }

    private int getImageHeight() {
        return maxWordGuesses * ((BOX_PADDING_Y * 2) + BOX_SIZE);
    }
}
