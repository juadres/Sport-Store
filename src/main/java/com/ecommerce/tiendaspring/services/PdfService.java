// Este es el código COMPLETO que reemplaza tu PdfService actual
package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.Venta;
import com.ecommerce.tiendaspring.models.DetalleVenta;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generarFacturaPdf(Venta venta) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Configuración de fuentes
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        
        // Encabezado mejorado
        Paragraph title = new Paragraph("FACTURA DE VENTA")
                .setFont(boldFont)
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Información de la empresa actualizada 2025
        Paragraph empresa = new Paragraph("SPORTSTORE 2025")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(empresa);

        Paragraph slogan = new Paragraph("Tu Tienda Deportiva de Confianza")
                .setFont(font)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(slogan);

        // Línea separadora
        document.add(new Paragraph("________________________________________________")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        // Información de la empresa
        Paragraph infoEmpresa = new Paragraph("SportStore - Equipamiento Deportivo Premium")
                .setFont(boldFont)
                .setFontSize(12);
        document.add(infoEmpresa);

        Paragraph direccion = new Paragraph("Bogotá, Colombia | Tel: +57 3116182363")
                .setFont(font)
                .setFontSize(10);
        document.add(direccion);

        Paragraph contacto = new Paragraph("Email: info@sportstore.com | Web: www.sportstore.com")
                .setFont(font)
                .setFontSize(10);
        document.add(contacto);

        document.add(new Paragraph(" "));

        // Tabla de información de la factura
        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));

        infoTable.addCell(crearCelda("N° FACTURA:", true));
        infoTable.addCell(crearCelda(venta.getNumeroFactura(), false));
        
        infoTable.addCell(crearCelda("FECHA DE EMISIÓN:", true));
        infoTable.addCell(crearCelda(
            venta.getFechaVenta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), false));
        
        infoTable.addCell(crearCelda("CLIENTE:", true));
        infoTable.addCell(crearCelda(venta.getUsuario().getNombre().toUpperCase(), false));
        
        infoTable.addCell(crearCelda("EMAIL:", true));
        infoTable.addCell(crearCelda(venta.getUsuario().getEmail(), false));

        document.add(infoTable);
        document.add(new Paragraph(" "));

        // Tabla de productos
        Paragraph detalleTitle = new Paragraph("DETALLE DE PRODUCTOS")
                .setFont(boldFont)
                .setFontSize(14)
                .setMarginBottom(10);
        document.add(detalleTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 2, 2}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Encabezados de tabla
        table.addHeaderCell(crearCelda("PRODUCTO DEPORTIVO", true));
        table.addHeaderCell(crearCelda("CANT", true));
        table.addHeaderCell(crearCelda("PRECIO UNIT.", true));
        table.addHeaderCell(crearCelda("SUBTOTAL", true));

        DecimalFormat df = new DecimalFormat("$#,##0.00");
        for (DetalleVenta detalle : venta.getDetalles()) {
            table.addCell(crearCelda(detalle.getProducto().getNombre(), false));
            table.addCell(crearCelda(detalle.getCantidad().toString(), false));
            table.addCell(crearCelda(df.format(detalle.getPrecioUnitario()), false));
            table.addCell(crearCelda(df.format(detalle.getSubtotal()), false));
        }

        document.add(table);
        document.add(new Paragraph(" "));

        // Tabla de totales
        Table totalesTable = new Table(2);
        totalesTable.setWidth(UnitValue.createPercentValue(50));
        totalesTable.setMarginLeft(250f);

        totalesTable.addCell(crearCelda("SUBTOTAL:", true));
        totalesTable.addCell(crearCelda(df.format(venta.getSubtotal()), false));
        
        totalesTable.addCell(crearCelda("IVA (19%):", true));
        totalesTable.addCell(crearCelda(df.format(venta.getIva()), false));
        
        totalesTable.addCell(crearCelda("TOTAL A PAGAR:", true));
        totalesTable.addCell(crearCelda(df.format(venta.getTotal()), true));

        document.add(totalesTable);

        document.add(new Paragraph(" "));
        
        // Mensaje de agradecimiento
        Paragraph agradecimiento = new Paragraph("¡Gracias por confiar en SportStore!")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(agradecimiento);

        Paragraph garantia = new Paragraph("Todos nuestros productos cuentan con garantía de calidad")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(garantia);

        Paragraph contacto2 = new Paragraph("Para consultas: soporte@sportstore.com | +57 3116182363")
                .setFont(font)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(contacto2);

        // Pie de página
        Paragraph footer = new Paragraph("SportStore 2025 - Todos los derechos reservados")
                .setFont(font)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(footer);

        document.close();
        return outputStream.toByteArray();
    }

    private Paragraph crearCelda(String texto, boolean negrita) {
        try {
            PdfFont font = negrita ? 
                PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD) :
                PdfFontFactory.createFont(StandardFonts.HELVETICA);
            
            return new Paragraph(texto)
                    .setFont(font)
                    .setFontSize(10)
                    .setMargin(0)
                    .setPadding(0);
        } catch (IOException e) {
            return new Paragraph(texto);
        }
    }
}