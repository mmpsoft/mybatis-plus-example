package com.maguasoft.example.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class LambdaSubQueryWrapper<T> extends AbstractLambdaWrapper<T, LambdaSubQueryWrapper<T>>
        implements Query<LambdaSubQueryWrapper<T>, T, SFunction<T, ?>> {

    public static final String TABLE_ALIASES = "sub";

    public static final String SQL = "select %s from %s " + TABLE_ALIASES + " %s";

    private SharedString sqlSelect = new SharedString();

    public LambdaSubQueryWrapper() {
        this((T) null);
    }

    public LambdaSubQueryWrapper(T entity) {
        super.setEntity(entity);
        super.initNeed();
    }

    public LambdaSubQueryWrapper(Class<T> entityClass) {
        super.setEntityClass(entityClass);
        super.initNeed();
    }

    public static <E> LambdaSubQueryWrapper<E> of(Class<E> entityClass) {
        return new LambdaSubQueryWrapper<>(entityClass);
    }

    LambdaSubQueryWrapper(T entity, Class<T> entityClass, SharedString sqlSelect, AtomicInteger paramNameSeq,
                          Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments, SharedString paramAlias,
                          SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.sqlSelect = sqlSelect;
        this.paramAlias = paramAlias;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
    }

    @Override
    public LambdaSubQueryWrapper<T> select(boolean condition, List<SFunction<T, ?>> columns) {
        return doSelect(condition, columns);
    }

    /**
     * 过滤查询的字段信息(主键除外!)
     * <p>例1: 只要 java 字段名以 "test" 开头的             -> select(i -&gt; i.getProperty().startsWith("test"))</p>
     * <p>例2: 只要 java 字段属性是 CharSequence 类型的     -> select(TableFieldInfo::isCharSequence)</p>
     * <p>例3: 只要 java 字段没有填充策略的                 -> select(i -&gt; i.getFieldFill() == FieldFill.DEFAULT)</p>
     * <p>例4: 要全部字段                                   -> select(i -&gt; true)</p>
     * <p>例5: 只要主键字段                                 -> select(i -&gt; false)</p>
     *
     * @param predicate 过滤方式
     * @return this
     */
    @Override
    public LambdaSubQueryWrapper<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        if (entityClass == null) {
            entityClass = getEntityClass();
        } else {
            setEntityClass(entityClass);
        }
        Assert.notNull(entityClass, "entityClass can not be null");
        this.sqlSelect.setStringValue(TableInfoHelper.getTableInfo(entityClass).chooseSelect(predicate));
        return typedThis;
    }

    @Override
    @SafeVarargs
    public final LambdaSubQueryWrapper<T> select(SFunction<T, ?>... columns) {
        return doSelect(true, CollectionUtils.toList(columns));
    }

    @Override
    @SafeVarargs
    public final LambdaSubQueryWrapper<T> select(boolean condition, SFunction<T, ?>... columns) {
        return doSelect(condition, CollectionUtils.toList(columns));
    }

    /**
     * @since 3.5.4
     */
    protected LambdaSubQueryWrapper<T> doSelect(boolean condition, List<SFunction<T, ?>> columns) {
        if (condition && CollectionUtils.isNotEmpty(columns)) {
            this.sqlSelect.setStringValue(columnsToString(false, columns));
        }
        return typedThis;
    }

    @Override
    public String getSqlSelect() {
        return sqlSelect.getStringValue();
    }

    /**
     * 用于生成嵌套 sql
     * <p>故 sqlSelect 不向下传递</p>
     */
    @Override
    protected LambdaSubQueryWrapper<T> instance() {
        return new LambdaSubQueryWrapper<>(getEntity(), getEntityClass(), null, paramNameSeq, paramNameValuePairs,
                new MergeSegments(), paramAlias, SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    @Override
    public void clear() {
        super.clear();
        sqlSelect.toNull();
    }

    public String getSql() {
        String customSqlSegment = getExpression().getSqlSegment();
        Set<String> sqlArgKeys = getParamNameValuePairs().keySet();

        // TODO 别名问题
        String formatWhereSql = "";
        if (StringUtils.isNotBlank(customSqlSegment)) {
            formatWhereSql = sqlArgKeys.stream().reduce(" where " + customSqlSegment, (whereSql, key) -> {
                String replaceRegex = String.format("#{%s.paramNameValuePairs.%s}", TABLE_ALIASES, key);
                Object originVal = getParamNameValuePairs().get(key);
                Object warpVal = originVal instanceof String ? String.format("'%s'", originVal) : originVal;
                return whereSql.replace(replaceRegex, Objects.toString(warpVal));
            });
        }

        TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
        return String.format(SQL, getAllColumns(), tableInfo.getTableName(), formatWhereSql);
    }

    private String getAllColumns() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(getEntityClass());
        String sqlSelect = StringUtils.isNotBlank(getSqlSelect()) ? getSqlSelect() : tableInfo.getAllSqlSelect();
        return Arrays.stream(sqlSelect.split(",")).map(it -> TABLE_ALIASES + "." + it).collect(Collectors.joining(", "));
    }
}
