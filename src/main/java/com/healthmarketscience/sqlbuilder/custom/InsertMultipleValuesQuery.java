package com.healthmarketscience.sqlbuilder.custom;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.common.util.AppendeeObject;
import com.healthmarketscience.sqlbuilder.*;
import com.healthmarketscience.sqlbuilder.dbspec.Table;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InsertMultipleValuesQuery extends InsertQuery {
    private SqlObjectList<SqlObjectList<SqlObject>> queryValues;
    private final Converter<Object, SqlObject> valueToObjectConverter;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private boolean needEscapeQuotes;

    public InsertMultipleValuesQuery(Table table) {
        super(table);
        valueToObjectConverter = new Converter<Object, SqlObject>() {
            @Override
            public SqlObject convert(Object src) {
                if (src instanceof Date) {
                    return new ValueObject(dateFormat.format(src));
                }

                if (src instanceof String) {
                    return !((String) src).isEmpty() ? new ValueObject(validateInput(String.valueOf(src))) : SqlObject.NULL_VALUE;
                }

                return toValueSqlObject(src);
            }
        };
    }

    private String validateInput(String input) {
        return StringEscapeUtils.escapeSql(input);
    }

    public InsertMultipleValuesQuery addColumns(Collection<DbColumn> columns, List<List<Object>> values) {
        queryValues = SqlObjectList.create();
        _columns = SqlObjectList.create();

        _columns.addObjects(Converter.CUSTOM_COLUMN_TO_OBJ, columns.toArray());

        for (List<Object> row : values) {
            SqlObjectList<SqlObject> rowOfValues = SqlObjectList.create();
            rowOfValues.addObjects(valueToObjectConverter, row.toArray());
            queryValues.addObject(rowOfValues);
        }

        return this;
    }

    @Override
    protected void appendTo(AppendableExt app, SqlContext newContext) throws IOException {
        newContext.setUseTableAliases(false);

        appendPrefixTo(app);

        String values = StreamSupport.stream(queryValues.spliterator(), false)
                .map(row -> StreamSupport.stream(row.spliterator(), false)
                        .map(AppendeeObject::toString)
                        .collect(Collectors.joining(",", "(", ")")))
                .collect(Collectors.joining(","));

        app.append("VALUES ").append(values);
    }

    @Override
    public void validate(ValidationContext vContext) throws ValidationException {
    }

    public SqlObjectList<SqlObjectList<SqlObject>> getQueryValues() {
        return queryValues;
    }

    public Converter<Object, SqlObject> getValueToObjectConverter() {
        return valueToObjectConverter;
    }

    public void setNeedEscapeQuotes(boolean needEscapeQuotes) {
        this.needEscapeQuotes = needEscapeQuotes;
    }
}
