package hotel.reports;

import hotel.model.Bill;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

// Generates a PDF bill using JasperReports 6.21.0
// Abstraction: callers only invoke print(); JRXML generation, compilation, and export are hidden
public class BillReport {

    // Writes JRXML to file, compiles it, fills with bill parameters, then exports to PDF
    public static String print(Bill bill) {
        try {
            String folder = System.getProperty("user.dir") + File.separator + "receipts";
            new File(folder).mkdirs();

            String jrxmlPath = folder + File.separator + "Bill_" + bill.getId() + ".jrxml";
            String pdfPath   = folder + File.separator + "Bill_" + bill.getId() + ".pdf";

            // STEP 1: Generate JRXML as string and write to file
            generateJrxml(jrxmlPath, bill);

            // STEP 2: Compile the JRXML file
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlPath);

            // STEP 3: Build parameters map with all bill data
            Map<String, Object> params = new HashMap<>();
            params.put("BILL_ID",        String.valueOf(bill.getId()));
            params.put("RESERVATION_ID", String.valueOf(bill.getReservationId()));
            params.put("CUSTOMER_NAME",  bill.getCustomerName());
            params.put("ROOM_NUMBER",    bill.getRoomNumber());
            params.put("ROOM_TYPE",      nvl(bill.getRoomType()));
            params.put("CHECK_IN",       bill.getCheckIn());
            params.put("CHECK_OUT",      bill.getCheckOut());
            params.put("NIGHTS",         String.valueOf(bill.getNights()));
            params.put("ROOM_PRICE",     String.format("%.2f", bill.getRoomPrice()));
            params.put("SUBTOTAL",       String.format("%.2f", bill.getSubtotal()));
            params.put("DISCOUNT",       String.format("%.2f", bill.getDiscount()));
            params.put("TAX",            String.format("%.2f", bill.getTax()));
            params.put("GRAND_TOTAL",    String.format("%.2f", bill.getGrandTotal()));
            params.put("PAYMENT_STATUS", bill.isPaid() ? "PAID" : "UNPAID");
            params.put("PAYMENT_METHOD", nvl(bill.getPaymentMethod()));
            params.put("ISSUED_AT",      bill.getIssuedAt());

            // STEP 4: Fill with empty datasource (all data is in params)
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport, params, new JREmptyDataSource());

            // STEP 5: Export to PDF
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(pdfPath));
            exporter.exportReport();

            System.out.println("[Report] PDF saved: " + pdfPath);

            // STEP 6: Auto-open the PDF
            File pdf = new File(pdfPath);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdf);
            }

            return pdfPath;

        } catch (Exception e) {
            System.err.println("[Report] Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void generateJrxml(String outputPath, Bill bill) throws Exception {
        String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\"\n" +
            "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "    xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports\n" +
            "    http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\"\n" +
            "    name=\"BillReceipt\" pageWidth=\"595\" pageHeight=\"842\"\n" +
            "    columnWidth=\"515\" leftMargin=\"40\" rightMargin=\"40\"\n" +
            "    topMargin=\"40\" bottomMargin=\"40\"\n" +
            "    whenNoDataType=\"AllSectionsNoDetail\">\n" +
            "\n" +
            "    <parameter name=\"BILL_ID\"        class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"RESERVATION_ID\" class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"CUSTOMER_NAME\"  class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"ROOM_NUMBER\"    class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"ROOM_TYPE\"      class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"CHECK_IN\"       class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"CHECK_OUT\"      class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"NIGHTS\"         class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"ROOM_PRICE\"     class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"SUBTOTAL\"       class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"DISCOUNT\"       class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"TAX\"            class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"GRAND_TOTAL\"    class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"PAYMENT_STATUS\" class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"PAYMENT_METHOD\" class=\"java.lang.String\"/>\n" +
            "    <parameter name=\"ISSUED_AT\"      class=\"java.lang.String\"/>\n" +
            "\n" +
            "    <title>\n" +
            "        <band height=\"580\" splitType=\"Stretch\">\n" +
            "\n" +
            "            <!-- Hotel Name -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"0\" width=\"515\" height=\"36\"/>\n" +
            "                <textElement textAlignment=\"Center\" verticalAlignment=\"Middle\">\n" +
            "                    <font size=\"22\" isBold=\"true\"/>\n" +
            "                </textElement>\n" +
            "                <text><![CDATA[SIXEVEN HOTEL]]></text>\n" +
            "            </staticText>\n" +
            "\n" +
            "            <line>\n" +
            "                <reportElement x=\"0\" y=\"60\" width=\"515\" height=\"1\"/>\n" +
            "                <graphicElement><pen lineWidth=\"1.5\"/></graphicElement>\n" +
            "            </line>\n" +
            "\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"66\" width=\"515\" height=\"22\"/>\n" +
            "                <textElement textAlignment=\"Center\">\n" +
            "                    <font size=\"13\" isBold=\"true\"/>\n" +
            "                </textElement>\n" +
            "                <text><![CDATA[OFFICIAL RECEIPT]]></text>\n" +
            "            </staticText>\n" +
            "\n" +
            "            <line>\n" +
            "                <reportElement x=\"0\" y=\"94\" width=\"515\" height=\"1\"/>\n" +
            "                <graphicElement><pen lineWidth=\"0.5\"/></graphicElement>\n" +
            "            </line>\n" +
            "\n" +
            "            <!-- Bill Number -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"104\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Bill Number:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"104\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{BILL_ID}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Reservation No -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"124\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Reservation No.:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"124\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{RESERVATION_ID}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Customer -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"144\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Customer:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"144\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{CUSTOMER_NAME}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Room -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"164\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Room:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"164\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{ROOM_NUMBER} + \" (\" + $P{ROOM_TYPE} + \")\"]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Check-In -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"184\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Check-In:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"184\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{CHECK_IN}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Check-Out -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"204\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Check-Out:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"204\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{CHECK_OUT}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Nights -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"224\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Number of Nights:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"224\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{NIGHTS}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Rate per Night -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"244\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Rate per Night:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"244\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[\"P\" + $P{ROOM_PRICE}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Date Issued -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"264\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Date Issued:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"264\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{ISSUED_AT}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Payment Method -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"284\" width=\"200\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Payment Method:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"200\" y=\"284\" width=\"315\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[$P{PAYMENT_METHOD}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <line>\n" +
            "                <reportElement x=\"0\" y=\"316\" width=\"515\" height=\"1\"/>\n" +
            "                <graphicElement><pen lineWidth=\"0.5\"/></graphicElement>\n" +
            "            </line>\n" +
            "\n" +
            "            <!-- Subtotal -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"324\" width=\"300\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\" isBold=\"true\"/></textElement>\n" +
            "                <text><![CDATA[Subtotal:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"300\" y=\"324\" width=\"215\" height=\"18\"/>\n" +
            "                <textElement textAlignment=\"Right\"><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[\"P\" + $P{SUBTOTAL}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Discount -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"344\" width=\"300\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <text><![CDATA[Discount:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"300\" y=\"344\" width=\"215\" height=\"18\"/>\n" +
            "                <textElement textAlignment=\"Right\"><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[\"- P\" + $P{DISCOUNT}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Tax -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"364\" width=\"300\" height=\"18\"/>\n" +
            "                <textElement><font size=\"10\"/></textElement>\n" +
            "                <text><![CDATA[Tax:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"300\" y=\"364\" width=\"215\" height=\"18\"/>\n" +
            "                <textElement textAlignment=\"Right\"><font size=\"10\"/></textElement>\n" +
            "                <textFieldExpression><![CDATA[\"+ P\" + $P{TAX}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <line>\n" +
            "                <reportElement x=\"0\" y=\"390\" width=\"515\" height=\"1\"/>\n" +
            "                <graphicElement><pen lineWidth=\"1.5\"/></graphicElement>\n" +
            "            </line>\n" +
            "\n" +
            "            <!-- Grand Total -->\n" +
            "            <staticText>\n" +
            "                <reportElement x=\"0\" y=\"398\" width=\"300\" height=\"28\"/>\n" +
            "                <textElement verticalAlignment=\"Middle\">\n" +
            "                    <font size=\"15\" isBold=\"true\"/>\n" +
            "                </textElement>\n" +
            "                <text><![CDATA[GRAND TOTAL:]]></text>\n" +
            "            </staticText>\n" +
            "            <textField>\n" +
            "                <reportElement x=\"300\" y=\"398\" width=\"215\" height=\"28\"/>\n" +
            "                <textElement textAlignment=\"Right\" verticalAlignment=\"Middle\">\n" +
            "                    <font size=\"15\" isBold=\"true\"/>\n" +
            "                </textElement>\n" +
            "                <textFieldExpression><![CDATA[\"P\" + $P{GRAND_TOTAL}]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <!-- Payment Status -->\n" +
            "            <textField>\n" +
            "                <reportElement x=\"0\" y=\"440\" width=\"515\" height=\"24\"/>\n" +
            "                <textElement textAlignment=\"Center\">\n" +
            "                    <font size=\"13\" isBold=\"true\"/>\n" +
            "                </textElement>\n" +
            "                <textFieldExpression><![CDATA[\"[ \" + $P{PAYMENT_STATUS} + \" ]\"]]></textFieldExpression>\n" +
            "            </textField>\n" +
            "\n" +
            "            <line>\n" +
            "                <reportElement x=\"0\" y=\"476\" width=\"515\" height=\"1\"/>\n" +
            "                <graphicElement><pen lineWidth=\"0.5\"/></graphicElement>\n" +
            "            </line>\n" +
            "\n" +
            "        </band>\n" +
            "    </title>\n" +
            "\n" +
            "</jasperReport>\n";

        try (FileWriter fw = new FileWriter(outputPath)) {
            fw.write(xml);
        }
        System.out.println("[Report] JRXML written to: " + outputPath);
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}
