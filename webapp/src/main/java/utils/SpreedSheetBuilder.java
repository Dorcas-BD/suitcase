package utils;

import model.SpreedSheetData;
import model.serialization.Column;
import model.serialization.Row;
import model.serialization.TableInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kamil Kalfas
 * kkalfas@soldevelo.com
 * Date: 5/20/15
 * Time: 6:27 PM
 */
public class SpreedSheetBuilder {

    private SpreedSheetData mSheetData;

    public SpreedSheetBuilder(TableInfo tableInfo) {
        mSheetData = prepareDataForSpreedSheet(tableInfo);
    }

    public String buildSpreedSheet() {
        StringBuilder html = new StringBuilder();
        html.append("<style type=\"text/css\">\n" +
                ".report  {border-collapse:collapse;border-spacing:0;}\n" +
                ".report td{font-family:Arial, sans-serif;font-size:14px;padding:10px 5px;border-style:solid;border-width:1px;overflow:hidden;word-break:normal;}\n" +
                ".report th{font-family:Arial, sans-serif;font-size:14px;font-weight:normal;padding:10px 5px;border-style:solid;border-width:1px;overflow:hidden;word-break:normal;}\n" +
                ".report .label{text-align:center}\n" +
                "</style>");
        html.append("<table class='report'>");
        html.append("<thead>")
                .append("<th collspan='" + mSheetData.getColumnsNumb() + "'>"
                        + mSheetData.getFormName() + "</th>").append("</thead>")
                .append("<tbody>");

        int formIndex = 1;
        for (List<SpreedSheetColumn> row : mSheetData.getSpreedSheetColumns()) {
            // building first three rows
            if (formIndex == 1) {
                html.append("<tr><td></td>");
                for (SpreedSheetColumn column : row) {
                    html.append("<td class='label' collspan='4'>" + column.getFieldLabelColumn().getValue() + "</td>");
                }
                html.append("</tr>");
                html.append("<tr><td>" + SpreedSheetData.LABELS_ROW[0] + "</td>");
                for (int i = 0; i < mSheetData.getColumnsNumb(); i++) {
                    html.append("<td>" + SpreedSheetData.LABELS_ROW[1] + "</td>");
                    html.append("<td>" + SpreedSheetData.LABELS_ROW[2] + "</td>");
                    html.append("><td>" + SpreedSheetData.LABELS_ROW[3] + "</td>");
                    html.append("<td>" + SpreedSheetData.LABELS_ROW[4] + "</td>");
                }
                html.append("</tr>");
            }
            // building rows with data
            html.append("<tr><td>" + formIndex + "</td>");
            for (SpreedSheetColumn column : row) {
                html.append("<td>" + column.getFieldLabelColumn().getValue() + "</td>");
                html.append("<td>" + "TBD"/*column.getRawValueColumn().getValue()*/ + "</td>");
                html.append("<td>" + column.getFinalValueColumn().getValue() + "</td>");
                html.append("<td><img src='" + column.getImageColumn().getValue() + "'></td>");
            }
            html.append("</tr>");
            formIndex++;
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private SpreedSheetData prepareDataForSpreedSheet(TableInfo info) {
        List<List<SpreedSheetColumn>> sheetRow = new ArrayList<List<SpreedSheetColumn>>();
        for (Row row : info.getRowList()) {
            List<Column> orderedColumns = row.getOrderedColumns();
            List<SpreedSheetColumn> rowColumns = new ArrayList<SpreedSheetColumn>();
            for (int i = 0; i < orderedColumns.size(); i += 3) {
                // column orderedColumns.get(i) creates two columns
                // column orderedColumns.get(i + 1) contains type of field AND is redundant IMO 
                // orderedColumns.get(i + 2) contains image file name value
                String imgURL = info.getInstanceFilesURL() + File.separator + row.getRowID() + File.separator +
                        "file/" + orderedColumns.get(i + 2).getColumnValue();
                rowColumns.add(generateSheetColumn(orderedColumns.get(i), imgURL));
            }
            sheetRow.add(rowColumns);
        }

        return new SpreedSheetData(info.getTableID(), sheetRow);
    }


    private SpreedSheetColumn generateSheetColumn(Column fieldLabelAndFinalValue, String imgURL) {
        AbstractColumn labelColumn = new AbstractColumn(fieldLabelAndFinalValue.getColumnName());
        AbstractColumn rawColumn = null;
        AbstractColumn finalColumn = new AbstractColumn(fieldLabelAndFinalValue.getColumnValue());
        AbstractColumn imageColumn = new AbstractColumn(imgURL);
        return new SpreedSheetColumn(labelColumn, rawColumn, finalColumn, imageColumn);
    }


    public class SpreedSheetColumn {
        private AbstractColumn mFieldLabelColumn;
        private AbstractColumn mRawValueColumn;
        private AbstractColumn mFinalValueColumn;
        private AbstractColumn mImageColumn;

        public SpreedSheetColumn(AbstractColumn fieldLabelColumn, AbstractColumn rawValueColumn, AbstractColumn finalValueColumn, AbstractColumn imageColumn) {
            mFieldLabelColumn = fieldLabelColumn;
            mRawValueColumn = rawValueColumn;
            mFinalValueColumn = finalValueColumn;
            mImageColumn = imageColumn;
        }

        public AbstractColumn getFieldLabelColumn() {
            return mFieldLabelColumn;
        }

        public AbstractColumn getRawValueColumn() {
            return mRawValueColumn;
        }

        public AbstractColumn getFinalValueColumn() {
            return mFinalValueColumn;
        }

        public AbstractColumn getImageColumn() {
            return mImageColumn;
        }
    }

    public class AbstractColumn {
        protected String value;

        public AbstractColumn(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
