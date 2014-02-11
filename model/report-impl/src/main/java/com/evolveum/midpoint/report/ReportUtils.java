package com.evolveum.midpoint.report;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRStyle;
import net.sf.jasperreports.engine.base.JRBasePen;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignReportTemplate;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;


public class ReportUtils {

	private static String MIDPOINT_HOME = System.getProperty("midpoint.home"); 
    private static String EXPORT_DIR = MIDPOINT_HOME + "export/";
    
    // parameters define objectQuery
    private static String PARAMETER_OBJECT_TYPE = "type";
    private static String PARAMETER_QUERY_FILTER = "filter";
    
    // parameter HQL query for jasper design (queryString element)
    private static String PARAMETER_HQLQUERY = "hqlQuery";
    
    private static String PARAMETER_LOGO = "logoPath";
    private static String PARAMETER_TEMPLATE_STYLES = "baseTemplateStyles";
    
    private static final Trace LOGGER = TraceManager
			.getTrace(ReportUtils.class);

    
	public static Class getClassType(QName clazz)
    {
		Class classType = java.lang.String.class; 
    	try
    	{
    		classType = XsdTypeMapper.getXsdToJavaMapping(clazz);
    		if (classType == javax.xml.datatype.XMLGregorianCalendar.class) {
    			classType = java.sql.Timestamp.class;
    		}
    		classType = (classType == null) ? java.lang.String.class : classType ;
    	} catch (Exception ex){
    		classType = java.lang.String.class;
    	}
    	return classType;
    	
    }

    public static Element getParametersXsdSchema(ReportType reportType) {
		XmlSchemaType xmlSchemaType = reportType.getConfigurationSchema();
		if (xmlSchemaType == null) {
			return null;
		}
		return ObjectTypeUtil.findXsdElement(xmlSchemaType);
	}
	
	public static PrismSchema getParametersSchema(ReportType reportType, PrismContext prismContext) throws SchemaException {
		Element parametersSchemaElement = getParametersXsdSchema(reportType);
		if (parametersSchemaElement == null) {
			return null;
		}
		PrismSchema parametersSchema = PrismSchema.parse(parametersSchemaElement, true, "schema for " + reportType, prismContext);
		if (parametersSchema == null) {
			throw new SchemaException("No parameters schema in "+ reportType);
		}
		return parametersSchema;
	}

    public static void applyDefinition(PrismObject<ReportType> report, PrismContext prismContext, boolean raw)
            throws SchemaException {
        if (raw) {
            return;
        }

        PrismContainer<Containerable> configuration = report.findContainer(ReportType.F_CONFIGURATION);
        if (configuration == null) {
            //nothing to apply definitions on
            return;
        }

        PrismContainer xmlSchema = report.findContainer(ReportType.F_CONFIGURATION_SCHEMA);
        Element xmlSchemaElement = ObjectTypeUtil.findXsdElement(xmlSchema);
        if (xmlSchemaElement == null) {
            //no schema definition available
            throw new SchemaException("Couldn't find schema for configuration in report type " + report + ".");
        }

        PrismSchema schema = PrismSchema.parse(xmlSchemaElement, true, "schema for " + report, prismContext);
        QName configContainerQName = new QName(schema.getNamespace(), ReportType.F_CONFIGURATION.getLocalPart());
        PrismContainerDefinition<ReportConfigurationType> definition = schema.findContainerDefinitionByElementName(configContainerQName);
        if (definition == null) {
            //no definition found for container
            throw new SchemaException("Couldn't find definitions for report type " + report + ".");
        }

        configuration.applyDefinition(definition, true);
    }
	
	
    public static PrismContainer<Containerable> getParametersContainer(ReportType reportType, PrismSchema schema)
			throws SchemaException, ObjectNotFoundException {
		
		PrismContainer<Containerable> configuration = reportType.asPrismObject().findContainer(ReportType.F_CONFIGURATION);
		if (configuration == null) {
			throw new SchemaException("No configuration container in " + reportType);
		}
		
		LOGGER.trace("Parameters container : {}", configuration.dump());
		
		QName configContainerQName = new QName(schema.getNamespace(), ReportType.F_CONFIGURATION.getLocalPart());
		PrismContainerDefinition<ReportConfigurationType> configurationContainerDefinition = schema.findContainerDefinitionByElementName(configContainerQName);
		
		if (configurationContainerDefinition == null) {
			throw new SchemaException("No configuration container definition in " + reportType);
		}
		
		LOGGER.trace("Parameters configuration definition: {}", configurationContainerDefinition.dump());
		
		configuration.applyDefinition(configurationContainerDefinition, true);
		
		LOGGER.trace("Parameters container with definitions : {}", configuration.dump());
		 
		return configuration;
	}
    
    public static Map<String, Object> getReportParams(ReportType reportType, PrismContainer<Containerable> parameterConfiguration, OperationResult parentResult)
	{
		Map<String, Object> params = new HashMap<String, Object>();
		 	
		OperationResult subResult = parentResult.createSubresult("get report parameters");
		if (parameterConfiguration != null) 	
		{
			for(PrismProperty parameter : parameterConfiguration.getValue().getProperties())
			{
				LOGGER.trace("parameter {} --- {}", parameter.getElementName().getLocalPart(), parameter.getRealValue());
				if ((parameter.getElementName().getLocalPart() != PARAMETER_OBJECT_TYPE) && (parameter.getElementName().getLocalPart() != PARAMETER_QUERY_FILTER))
				{
					params.put(parameter.getElementName().getLocalPart(), parameter.getRealValue());
					Class classType = ReportUtils.getClassType(parameter.getDefinition().getTypeName());
					if (classType == java.sql.Timestamp.class) {
						params.put(parameter.getElementName().getLocalPart(), ReportUtils.convertDateTime((XMLGregorianCalendar)parameter.getRealValue(XMLGregorianCalendar.class)));
					}
				}
			}
		}
		  	
		subResult.computeStatus();	
		 
		return params;
	}
    
	public static Class getObjectTypeClass(ReportType reportType, PrismContainer<Containerable> parameterConfiguration, String namespace)
			throws SchemaException, ObjectNotFoundException {
		
		PrismProperty objectTypeProp = getParameter(PARAMETER_OBJECT_TYPE, parameterConfiguration, namespace);
		Class clazz = ObjectType.class;
		if (objectTypeProp != null)
		{
			try
			{
				QName objectType = (QName) objectTypeProp.getRealValue();
				
				LOGGER.trace("Parameter object type : {}", objectType);
				
				clazz = ObjectTypes.getObjectTypeClass(objectType.getLocalPart());
				
				LOGGER.trace("Parameter clas of object type : {}", clazz);
				
			} catch (Exception ex){
				LOGGER.trace("not object type parameter");
			}
		}
		return clazz;
	}
	
	private static PrismProperty getParameter(String parameterName, PrismContainer<Containerable> parameterConfiguration, String namespace)
	{
		PrismProperty property = parameterConfiguration.findProperty(new QName(namespace, parameterName));	
		for(PrismProperty parameter : parameterConfiguration.getValue().getProperties())
		{
			LOGGER.trace("Parameter : {} ", parameter);
			LOGGER.trace("Display Name : {}", parameter.getDisplayName());
			LOGGER.trace("Real value : {}", parameter.getRealValue());
			LOGGER.trace("Element Name : {}", parameter.getElementName());
			LOGGER.trace("Definition - type name: {}", parameter.getDefinition().getTypeName());
			LOGGER.trace("--------------------------------------------------------------------------------");
		}
		return property;
	}
	
	public static Timestamp convertDateTime(XMLGregorianCalendar dateTime)
	{
		//XMLGregorianCalendar xmlCal = XmlTypeConverter.createXMLGregorianCalendar(dateTime.toString());
		Timestamp timestamp = new Timestamp(System.currentTimeMillis()); 
		try {
			timestamp = new Timestamp(XmlTypeConverter.toDate(dateTime).getTime()); 
		}
		catch (Exception ex)
		{
			LOGGER.trace("Incorrect date time value {}", dateTime);
		}
		
		return timestamp;
	}
	
	private static JRDesignParameter createParameter(PrismProperty parameterConfig)
	{
		JRDesignParameter parameter = new JRDesignParameter();
		parameter.setName(parameterConfig.getElementName().getLocalPart());
		parameter.setValueClass(getClassType(parameterConfig.getDefinition().getTypeName()));
		return parameter;
	}
    
	private static JRDesignTextField createField(ReportFieldConfigurationType fieldRepo, int x, int width, int frameWidth)
	{
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(x);
		textField.setY(1);		
		textField.setWidth(width);
		textField.setHeight(18);
		textField.setStretchWithOverflow(true);
		textField.setBlankWhenNull(true);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Detail");
		textField.setExpression(new JRDesignExpression("$F{" + fieldRepo.getNameReportField() + "}"));
		return textField;
	}
	
	private static void setOrientation(JasperDesign jasperDesign, OrientationEnum orientation, int pageWidth, int pageHeight, int columnWidth)
	{
		jasperDesign.setOrientation(orientation);
		jasperDesign.setPageWidth(pageWidth);
		jasperDesign.setPageHeight(pageHeight);
		jasperDesign.setColumnWidth(columnWidth);
	}
    
	private static JRDesignStyle createStyle(String name, boolean isDefault, boolean isBold, JRStyle parentStyle, Color backcolor, Color forecolor, ModeEnum mode, HorizontalAlignEnum hAlign, VerticalAlignEnum vAlign, int fontSize, String pdfFontName, String pdfEncoding, boolean isPdfEmbedded)
	{
		JRDesignStyle style = new JRDesignStyle();
		style.setName(name);
		style.setDefault(isDefault);
		style.setBold(isBold);
		if (parentStyle != null) style.setParentStyle(parentStyle);
		style.setBackcolor(backcolor);
		style.setForecolor(forecolor);
		if (hAlign != null) style.setHorizontalAlignment(hAlign);
		if (vAlign != null) style.setVerticalAlignment(vAlign);
		if (fontSize != 0) style.setFontSize(fontSize);
		if (mode != null) style.setMode(mode);
		if (!pdfFontName.isEmpty())
		{
			style.setPdfFontName(pdfFontName);
			style.setPdfEncoding(pdfEncoding);
			style.setPdfEmbedded(isPdfEmbedded);
		}
		return style;
	}
	
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle, Color backcolor, Color forecolor, ModeEnum mode, HorizontalAlignEnum hAlign, int fontSize)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, backcolor, forecolor, mode, hAlign, null, fontSize, "", "", false);
		return style;
	}
	
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle, Color backcolor, Color forecolor, ModeEnum mode, int fontSize)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, backcolor, forecolor, mode, null, null, fontSize, "", "", false);
		return style;
	}
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle, int fontSize)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, null, null, null, null, null, fontSize, "", "", false);
		return style;
	}
	
	private static JRDesignStyle createStyle(String name, boolean isBold, JRStyle parentStyle)
	{
		JRDesignStyle style = createStyle(name, false, isBold, parentStyle, null, null, null, null, null, 0, "", "", false);
		return style;
	}
	
	private static JRDesignBand createBand(int height, SplitTypeEnum split)
	{
		JRDesignBand band = new JRDesignBand();
		band.setHeight(height);
		band.setSplitType(split);
		return band;
	}
	
	private static JRDesignBand createBand(int height)
	{
		return createBand(height, SplitTypeEnum.STRETCH);
	}
	
	private static JRDesignFrame createFrame(int x, int y, int height, int width, String styleName, ModeEnum mode)
	{
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(x);
		frame.setY(y);
		frame.setHeight(height);
		frame.setWidth(width);
		frame.setStyleNameReference(styleName);
		if (mode != null) frame.setMode(mode);
		return frame;
	}
	
	private static JRDesignFrame createFrame(int x, int y, int height, int width, String styleName)
	{
		return createFrame(x, y, height, width, styleName, null);
	}
	private static JRDesignStaticText createStaticText(int x, int y, int height, int width, String styleName, VerticalAlignEnum vAlign, String text)
	{
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(x);
		staticText.setY(y);
		staticText.setHeight(height);
		staticText.setWidth(width);
		staticText.setStyleNameReference(styleName);
		staticText.setVerticalAlignment(vAlign);
		staticText.setText(text);
		return staticText;
	}
	
	private static JRDesignImage createImage(int x, int y, int height, int width, String styleName, JRExpression expression)
	{
		JRDesignImage image = new JRDesignImage(new JRDesignStyle().getDefaultStyleProvider());
		image.setX(x);
		image.setY(y);
		image.setHeight(height);
		image.setWidth(width);
		image.setStyleNameReference(styleName);
		image.setExpression(expression);
		return image;
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, HorizontalAlignEnum hAlign, VerticalAlignEnum vAlign, String styleName, Boolean isBold, EvaluationTimeEnum evalution, Boolean blankWhenNull, JRExpression expression)
	{
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(x);
		textField.setY(y);
		textField.setHeight(height);
		textField.setWidth(width);
		textField.setHorizontalAlignment(hAlign);
		textField.setVerticalAlignment(vAlign);
		textField.setStyleNameReference(styleName);
		if (isBold != null) textField.setBold(isBold);
		textField.setEvaluationTime(evalution);
		if (blankWhenNull != null) textField.setBlankWhenNull(blankWhenNull);
		textField.setExpression(expression);
		return textField;
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, Boolean isBold, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, isBold, EvaluationTimeEnum.NOW, null, expression);
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, null, EvaluationTimeEnum.NOW, null, expression);
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, Boolean isBold, EvaluationTimeEnum evalution, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, isBold, evalution, null, expression);
	}
	
	private static JRDesignTextField createTextField(int x, int y, int height, int width, String styleName, EvaluationTimeEnum evalution, JRExpression expression)
	{
		return createTextField(x, y, height, width, HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE, styleName, null, evalution, null, expression);
	}
	
	private static JRDesignLine createLine(int x, int y, int height, int width, PositionTypeEnum position, float penLineWidth)
	{
		JRDesignLine line = new JRDesignLine();
		line.setX(x);
		line.setY(y);
		line.setHeight(height);
		line.setWidth(width);
		line.setPositionType(position);
		JRBasePen pen = new JRBasePen(line);
		pen.setLineWidth(penLineWidth);
		pen.setLineColor(Color.decode("#999999"));
		return line;
	}
	
	private static void createStyles(JasperDesign jasperDesign) throws JRException
	{
		JRDesignStyle baseStyle = createStyle("Base", true, true, null, Color.decode("#FFFFFF"), Color.decode("#000000"), null, HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE, 10, "Helvetica", "Cp1252", false);
		jasperDesign.addStyle(baseStyle);
			
		JRDesignStyle titleStyle = createStyle("Title", true, baseStyle, Color.decode("#267994"), Color.decode("#FFFFFF"), ModeEnum.OPAQUE, 26);
		jasperDesign.addStyle(titleStyle);
		
		JRDesignStyle pageHeaderStyle = createStyle("Page header", true, baseStyle, 12);
		jasperDesign.addStyle(pageHeaderStyle);
		
		JRDesignStyle columnHeaderStyle = createStyle ("Column header", true, baseStyle, Color.decode("#333333"), Color.decode("#FFFFFF"), ModeEnum.OPAQUE, HorizontalAlignEnum.CENTER, 12);
		jasperDesign.addStyle(columnHeaderStyle);
		
		JRDesignStyle detailStyle = createStyle("Detail", false, baseStyle);
		jasperDesign.addStyle(detailStyle);
		
		JRDesignStyle pageFooterStyle = createStyle("Page footer", true, baseStyle, 9);
		jasperDesign.addStyle(pageFooterStyle);
	}

	private static JRDesignBand createTitleBand(int height, int reportColumn, int secondColumn, PrismContainer<Containerable> parameterConfiguration, PrismSchema prismSchema)
	{
		JRDesignBand titleBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 0, 70, reportColumn, "Title");
		titleBand.addElement(frame);
	
		JRDesignStaticText staticText = createStaticText(10, 15, 40, 266, "Title", VerticalAlignEnum.MIDDLE, "DataSource Report");
		frame.addElement(staticText);
		if (getParameter(PARAMETER_LOGO, parameterConfiguration, prismSchema.getNamespace()) != null) 
		{
			JRDesignImage image = createImage(589, 15, 40, 203, "Title", new JRDesignExpression("$P{" + PARAMETER_LOGO + "}"));
			frame.addElement(image);
		}
		staticText = createStaticText(secondColumn, 70, 20, 150, "Page header", VerticalAlignEnum.MIDDLE, "Report generated on:");
		titleBand.addElement(staticText);
	
		JRDesignTextField textField = createTextField(secondColumn + 150, 70, 20, 250, "Page header", false, new JRDesignExpression("new java.util.Date()"));
		titleBand.addElement(textField);
	
		staticText = createStaticText(secondColumn, 90, 20, 150, "Page header", VerticalAlignEnum.MIDDLE, "Number of records:");
		titleBand.addElement(staticText);
	
		textField = createTextField(secondColumn + 150, 90, 20, 250, "Page header", false, EvaluationTimeEnum.REPORT, new JRDesignExpression("$V{REPORT_COUNT}"));
		titleBand.addElement(textField);
	
	    //remove parameters, which are not special for data
		if (parameterConfiguration != null)
		{
			int y = 70;
			for(PrismProperty parameter : parameterConfiguration.getValue().getProperties())
			{
				LOGGER.trace("Parameter : {} ", parameter);
				LOGGER.trace("Display Name : {}", parameter.getDisplayName());
				LOGGER.trace("Real value : {}", parameter.getRealValue());
				LOGGER.trace("Element Name : {}", parameter.getElementName());
				LOGGER.trace("Definition : {}", parameter.getDefinition());
				LOGGER.trace("--------------------------------------------------------------------------------");
			
				if (parameter.getDisplayName() != null)
				{
					staticText = createStaticText(2, y, 20, 150, "Page header", VerticalAlignEnum.MIDDLE, parameter.getDisplayName() + ":");
					titleBand.addElement(staticText);
		
					textField = createTextField(160, y, 20, 240, "Page header", false, new JRDesignExpression("$P{"+ parameter.getElementName().getLocalPart() + "}"));
					titleBand.addElement(textField);

					y = y + 20;
				}
			}
		}
		return titleBand;
	}
	
	private static JRDesignBand createColumnHeaderBand(int height, int reportColumn, List<ReportFieldConfigurationType> reportFields)
	{
		JRDesignBand columnHeaderBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 5, 19, reportColumn, "Column header");
	
		int x = 0;
		int width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportFields)
		{
			width =  Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidthField()));	
			JRDesignStaticText staticText = createStaticText(x, 0, 18, width, "Column header", VerticalAlignEnum.MIDDLE, fieldRepo.getNameHeaderField());
			frame.addElement(staticText);
			x = x + width;
		}
	
		columnHeaderBand.addElement(frame);
		return columnHeaderBand;
	}
	
	private static JRDesignBand createDetailBand(int height, int reportColumn, List<ReportFieldConfigurationType> reportFields)
	{ 
		JRDesignBand detailBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 1, 19, reportColumn, "Detail");
	
		int x = 0;
		int width = 0;
		int frameWidth = frame.getWidth();
		for(ReportFieldConfigurationType fieldRepo : reportFields)
		{
			width = Math.round((float) ((frameWidth/100) * fieldRepo.getWidthField())); 
			JRDesignTextField textField = createField(fieldRepo, x, width, frameWidth);
			frame.addElement(textField);
			x = x + width;
		}
	
		JRDesignLine line = createLine(0, 3, 1, reportColumn, PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM, (float) 0.5);
		frame.addElement(line);
	
		detailBand.addElement(frame);
		return detailBand;
	}
	
	private static JRDesignBand createColumnFooterBand(int height, int reportColumn)
	{
		JRDesignBand columnFooterBand = createBand(height);
		
		JRDesignLine line = createLine(0, 3, 1, reportColumn, PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM, (float) 0.5);
		columnFooterBand.addElement(line);
		return columnFooterBand;
	}
	
	private static JRDesignBand createPageFooterBand(int height, int reportColumn)
	{
		JRDesignBand pageFooterBand = createBand(height);
		JRDesignFrame frame = createFrame(0, 1, 24, reportColumn, "Page footer", ModeEnum.TRANSPARENT);
		JRDesignTextField textField = createTextField(2, 1, 20, 197, "Page footer", new JRDesignExpression("new java.util.Date()"));
		frame.addElement(textField);

		textField = createTextField(680, 1, 20, 80, "Page footer", new JRDesignExpression("\"Page \" + String.valueOf($V{PAGE_NUMBER}) + \" of\""));
		frame.addElement(textField);
	
		textField = createTextField(760, 1, 20, 40,"Page footer", EvaluationTimeEnum.REPORT, new JRDesignExpression("$V{PAGE_NUMBER}")); 
		frame.addElement(textField);
	
		pageFooterBand.addElement(frame);
		return pageFooterBand;
	}
	
    public static JasperDesign createJasperDesign(ReportType reportType, PrismContainer<Containerable> parameterConfiguration, PrismSchema prismSchema) throws JRException
	{
    	
		//JasperDesign
		JasperDesign jasperDesign = new JasperDesign();
		String reportName = reportType.getName().getOrig(); 
		jasperDesign.setName(reportName.replace("\\s", ""));
		
		switch (reportType.getOrientation())
		{
			case LANDSCAPE :
			default: setOrientation(jasperDesign, OrientationEnum.LANDSCAPE, 842, 595, 802);
				break;
			case PORTRAIT :	setOrientation(jasperDesign, OrientationEnum.PORTRAIT, 595, 842, 555);
				break;
		}
		
		jasperDesign.setColumnSpacing(0);
		jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
		jasperDesign.setLeftMargin(20);
		jasperDesign.setRightMargin(20);
		jasperDesign.setTopMargin(20);
		jasperDesign.setBottomMargin(20);
		
		//Parameters
		if (parameterConfiguration != null)
		{
			for(PrismProperty parameterConfig : parameterConfiguration.getValue().getProperties())
			{
				JRDesignParameter parameter = createParameter(parameterConfig);
				jasperDesign.addParameter(parameter);	
			}
		}	 
		//Template Style or Styles
		if (getParameter(PARAMETER_TEMPLATE_STYLES, parameterConfiguration, prismSchema.getNamespace()) != null)
		{
			JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{" + PARAMETER_TEMPLATE_STYLES + "}"));
			jasperDesign.addTemplate(templateStyle);
		}
		else createStyles(jasperDesign);
		
		//Fields
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportField())
		{
			JRDesignField field = new JRDesignField();
			field.setName(fieldRepo.getNameReportField());
			field.setValueClass(getClassType(fieldRepo.getClassTypeField()));	
			jasperDesign.addField(field);
		}

		//Background
		JRDesignBand bandBackground = createBand(30);
		jasperDesign.setBackground(bandBackground);
		
		//Title
		//band size depends on the number of parameters
		int reportColumn = jasperDesign.getColumnWidth() - 2;
		int secondColumn = Math.round(jasperDesign.getColumnWidth()/2 - 1);
		//int height = 70 + Math.max(40, parameters.size()*20);
		int height = 70 + Math.max(40, 20);
		
		JRDesignBand titleBand = createTitleBand(height, reportColumn, secondColumn, parameterConfiguration, prismSchema);
		jasperDesign.setTitle(titleBand);
	
		//Column header
		JRDesignBand columnHeaderBand = createColumnHeaderBand(24, reportColumn, reportType.getReportField());
		jasperDesign.setColumnHeader(columnHeaderBand);
		
		//Detail
		JRDesignBand detailBand = createDetailBand(20, reportColumn, reportType.getReportField());
		((JRDesignSection)jasperDesign.getDetailSection()).addBand(detailBand);		
		
		//Column footer
		JRDesignBand columnFooterBand = createColumnFooterBand(7, reportColumn);
		jasperDesign.setColumnFooter(columnFooterBand);

		//Page footer
		JRDesignBand pageFooterBand = createPageFooterBand(32, reportColumn);
		jasperDesign.setPageFooter(pageFooterBand);

		return jasperDesign;
	}

       
    public static String getReportOutputFilePath(ReportType reportType){
    	
    	String output =  EXPORT_DIR + reportType.getName().getOrig();
    	switch (reportType.getExport())
        {
        	case PDF : output = output + ".pdf";
        		break;
          	case CSV : output = output + ".csv";
      			break;
          	case XML : output = output + ".xml";
          		break;
          	case XML_EMBED : output = output + "_embed.xml";
          		break;
          	case HTML : output = output + ".html";
          		break;
          	case RTF : output = output + ".rtf";
          		break;
          	case XLS : output = output + ".xls";
  				break;
          	case ODT : output = output + ".odt";
          		break;
          	case ODS : output = output + ".ods";
  				break;
          	case DOCX : output = output + ".docx";
  				break;
          	case XLSX : output = output + ".xlsx";
  				break;
          	case PPTX : output = output + ".pptx";
          		break;
          	case XHTML : output = output + ".x.html";
  				break;
          	case JXL : output = output + ".jxl.xls";
          		break; 	
			default:
				break;
        }
    	
    	return output;
    }
    
    public static String getDeltaAudit(String delta)
    {
    	String deltaAudit = null;
    	try
    	{
    		PrismContext prismContext = PrismTestUtil.createPrismContext();
    		ObjectDeltaType xmlDelta = prismContext.getPrismJaxbProcessor().unmarshalObject(delta, ObjectDeltaType.class);
    		deltaAudit = xmlDelta.getChangeType().toString() + " - " + xmlDelta.getObjectType().getLocalPart().toString();
    	} catch (Exception ex) {
    		return ex.getMessage();
    	}
    	
    	return deltaAudit;
    }
    
}