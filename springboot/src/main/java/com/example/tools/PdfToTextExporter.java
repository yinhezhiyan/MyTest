package com.example.tools;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 用途：将仓库根目录 docs/question-pdf 下的所有 PDF 批量提取为 txt
 * 输出：仓库根目录 docs/question-text 下同名 .txt
 *
 * 运行方式（两种）：
 * 1) 在 IDEA 里直接运行 main()
 * 2) mvn -f springboot/pom.xml -DskipTests exec:java (需要再配置 exec 插件，不推荐)
 *
 * 注意：
 * - 该工具假设你的仓库结构为：根目录/springboot/ 与 根目录/docs/ 同级
 */
public class PdfToTextExporter {

    public static void main(String[] args) {
        try {
            Path repoRoot = resolveRepoRoot();
            Path pdfDir = repoRoot.resolve("docs").resolve("question-pdf");
            Path outDir = repoRoot.resolve("docs").resolve("question-text");

            if (!Files.exists(pdfDir) || !Files.isDirectory(pdfDir)) {
                System.err.println("PDF目录不存在: " + pdfDir.toAbsolutePath());
                System.err.println("请确认 PDF 放在：docs/question-pdf/");
                return;
            }

            Files.createDirectories(outDir);

            try (Stream<Path> stream = Files.list(pdfDir)) {
                stream
                        .filter(p -> p.toString().toLowerCase().endsWith(".pdf"))
                        .sorted(Comparator.comparing(Path::getFileName))
                        .forEach(pdf -> {
                            Path txt = outDir.resolve(stripExt(pdf.getFileName().toString()) + ".txt");
                            System.out.println("Extract: " + pdf.getFileName() + " -> " + repoRoot.relativize(txt));
                            try {
                                extractOne(pdf, txt);
                            } catch (Exception e) {
                                System.err.println("提取失败: " + pdf.getFileName());
                                e.printStackTrace();
                            }
                        });
            }

            System.out.println("Done. 输出目录: " + outDir.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 从 springboot/ 目录推导仓库根目录：repoRoot = springboot/.. */
    private static Path resolveRepoRoot() {
        // 当前类运行时工作目录一般是 springboot/，但不完全可靠，所以做两层兜底：
        // 1) 如果当前目录下存在 springboot/pom.xml，则当前目录就是 repoRoot
        // 2) 如果当前目录名是 springboot 且包含 pom.xml，则 repoRoot 是父目录
        // 3) 否则尝试向上找 5 层，找到同时包含 springboot/ 和 docs/ 的目录作为 repoRoot
        Path cwd = Paths.get("").toAbsolutePath().normalize();

        // case 1: cwd 是 repoRoot
        if (Files.exists(cwd.resolve("springboot").resolve("pom.xml"))) {
            return cwd;
        }

        // case 2: cwd 是 springboot
        if (cwd.getFileName() != null
                && "springboot".equalsIgnoreCase(cwd.getFileName().toString())
                && Files.exists(cwd.resolve("pom.xml"))) {
            return cwd.getParent();
        }

        // case 3: 向上搜索
        Path p = cwd;
        for (int i = 0; i < 5; i++) {
            if (p == null) break;
            if (Files.exists(p.resolve("springboot").resolve("pom.xml"))
                    && Files.exists(p.resolve("docs"))) {
                return p;
            }
            p = p.getParent();
        }

        // 兜底：假设工作目录就是 repoRoot
        return cwd;
    }

    private static void extractOne(Path pdfPath, Path txtPath) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 你也可以设置页码范围：stripper.setStartPage(1); stripper.setEndPage(doc.getNumberOfPages());
            String text = stripper.getText(doc);
            text = normalize(text);

            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(txtPath.toFile()),
                    StandardCharsets.UTF_8
            )) {
                writer.write(text);
            }
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        // 替换不间断空格
        s = s.replace('\u00A0', ' ');
        // 统一换行
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        // 连续空行压缩
        s = s.replaceAll("\n{3,}", "\n\n");
        return s.trim() + "\n";
    }

    private static String stripExt(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(0, idx) : filename;
    }
}
