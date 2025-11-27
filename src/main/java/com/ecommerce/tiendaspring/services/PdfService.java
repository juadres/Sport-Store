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

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        Paragraph title = new Paragraph("FACTURA DE VENTA")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        document.add(new Paragraph(" "));

        // CAMBIO: Nombre de empresa a SportStore
        Paragraph empresa = new Paragraph("SportStore - Tu Tienda Deportiva")
                .setFont(boldFont)
                .setFontSize(12);
        document.add(empresa);

        Paragraph direccion = new Paragraph("Colombia, Bogotá")
                .setFont(font)
                .setFontSize(10);
        document.add(direccion);

        // CAMBIO: Email corporativo actualizado
        Paragraph contacto = new Paragraph("Tel: +57 1 1234567 | Email: info@sportstore.com")
                .setFont(font)
                .setFontSize(10);
        document.add(contacto);

        document.add(new Paragraph(" "));

        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));

        infoTable.addCell(crearCelda("N° Factura:", true));
        infoTable.addCell(crearCelda(venta.getNumeroFactura(), false));
        
        infoTable.addCell(crearCelda("Fecha:", true));
        infoTable.addCell(crearCelda(
            venta.getFechaVenta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), false));
        
        infoTable.addCell(crearCelda("Cliente:", true));
        infoTable.addCell(crearCelda(venta.getUsuario().getNombre(), false));
        
        infoTable.addCell(crearCelda("Email:", true));
        infoTable.addCell(crearCelda(venta.getUsuario().getEmail(), false));

        document.add(infoTable);
        document.add(new Paragraph(" "));

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 2, 2}));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(crearCelda("Producto", true));
        table.addHeaderCell(crearCelda("Cantidad", true));
        table.addHeaderCell(crearCelda("Precio Unit.", true));
        table.addHeaderCell(crearCelda("Subtotal", true));

        DecimalFormat df = new DecimalFormat("$#,##0.00");
        for (DetalleVenta detalle : venta.getDetalles()) {
            table.addCell(crearCelda(detalle.getProducto().getNombre(), false));
            table.addCell(crearCelda(detalle.getCantidad().toString(), false));
            table.addCell(crearCelda(df.format(detalle.getPrecioUnitario()), false));
            table.addCell(crearCelda(df.format(detalle.getSubtotal()), false));
        }

        document.add(table);
        document.add(new Paragraph(" "));

        Table totalesTable = new Table(2);
        totalesTable.setWidth(UnitValue.createPercentValue(50));
        totalesTable.setMarginLeft(250f);

        totalesTable.addCell(crearCelda("Subtotal:", true));
        totalesTable.addCell(crearCelda(df.format(venta.getSubtotal()), false));
        
        totalesTable.addCell(crearCelda("IVA (19%):", true));
        totalesTable.addCell(crearCelda(df.format(venta.getIva()), false));
        
        totalesTable.addCell(crearCelda("TOTAL:", true));
        totalesTable.addCell(crearCelda(df.format(venta.getTotal()), true));

        document.add(totalesTable);

        document.add(new Paragraph(" "));
        Paragraph agradecimiento = new Paragraph("¡Gracias por su compra!")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(agradecimiento);

        // CAMBIO: Email de soporte actualizado
        Paragraph contacto2 = new Paragraph("Para consultas: soporte@sportstore.com")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(contacto2);

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