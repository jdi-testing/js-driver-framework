package org.jdiai;

import com.epam.jdi.tools.LinqUtils;
import com.epam.jdi.tools.Timer;
import com.epam.jdi.tools.func.JAction1;
import com.epam.jdi.tools.func.JFunc1;
import org.jdiai.interfaces.HasCore;
import org.jdiai.interfaces.HasName;
import org.jdiai.interfaces.ISetup;
import org.jdiai.jsdriver.JSException;
import org.jdiai.tools.FilterCondition;
import org.openqa.selenium.By;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.*;

import static com.epam.jdi.tools.EnumUtils.getEnumValue;
import static com.epam.jdi.tools.LinqUtils.*;
import static com.epam.jdi.tools.ReflectionUtils.getGenericTypes;
import static com.epam.jdi.tools.ReflectionUtils.isClass;
import static org.jdiai.jswraper.JSWrappersUtils.getValueType;
import static org.jdiai.page.objects.PageFactoryUtils.getLocatorFromField;

public class DataList<T> implements List<T>, ISetup, HasCore, HasName {
    private JS core;
    private Class<T> dataClass;
    private String labelName;

    public T get(String value) {
        Field labelField = getLabelField();
        By labelLocator = getLocatorFromField(labelField);
        FilterCondition condition = getCondition(labelField, value);
        return core().findFirst(labelLocator, condition).getEntity();
    }
    private FilterCondition getCondition(Field labelField, String value) {
        FilterCondition condition = new FilterCondition(
            c -> getValueType(labelField, "element"));
        condition.value = value;
        return condition;
    }
    private Field getLabelField() {
        Field[] allFields = dataClass.getDeclaredFields();
        Field labelField = Arrays.stream(allFields).filter(
            f -> f.getName().equalsIgnoreCase(labelName)).findFirst().orElse(null);
        if (labelField != null) {
            return labelField;
        }
        return Arrays.stream(allFields).filter(
            f -> isClass(f.getType(), String.class))
            .findFirst().orElseGet(() -> allFields[0]);
    }
    private static Field getLabelField(Field labelField, Field[] allFields) {
        if (labelField != null) {
            return labelField;
        }
        return Arrays.stream(allFields).filter(
            f -> isClass(f.getType(), String.class))
            .findFirst().orElseGet(() -> allFields[0]);
    }

    public T get(Enum<?> name) { return get(getEnumValue(name)); }
    public T last() {
        return LinqUtils.last(getList(1));
    }
    public T first() {
        return get(0);
    }
    public List<T> where(JFunc1<T, Boolean> condition) {
        return LinqUtils.where(getList(0), condition);
    }
    public List<T> filter(JFunc1<T, Boolean> condition) {
        return where(condition);
    }
    public <R> List<R> select(JFunc1<T, R> transform) {
        return LinqUtils.select(getList(0), transform::execute);
    }
    public <R> List<R> map(JFunc1<T, R> transform) {
        return select(transform);
    }
    public T first(JFunc1<T, Boolean> condition) {
        return LinqUtils.first(getList(1), condition);
    }
    public T last(JFunc1<T, Boolean> condition) {
        return LinqUtils.last(getList(1), condition);
    }
    public void ifDo(JFunc1<T, Boolean> condition, JAction1<T> action) {
        LinqUtils.ifDo(getList(1), condition, action);
    }
    public <R> List<R> ifSelect(JFunc1<T, Boolean> condition, JFunc1<T, R> transform) {
        return LinqUtils.ifSelect(getList(1), condition, transform);
    }
    public void foreach(JAction1<T> action) {
        LinqUtils.foreach(getList(1), action);
    }
    public boolean hasAny(JFunc1<T, Boolean> condition) {
        return any(getList(0), condition);
    }
    public boolean all(JFunc1<T, Boolean> condition) {
        return LinqUtils.all(getList(0), condition);
    }
    public List<T> slice(int from, int to) {
        return listCopy(getList(to - 1), from, to);
    }
    public List<T> slice(int from) {
        return listCopy(getList(from - 1), from);
    }
    public List<T> sliceTo(int to) {
        return listCopyUntil(getList(to - 1), to);
    }
    public void refresh() { clear(); }
    public <R> List<R> selectMany(JFunc1<T, List<R>> func) {
        return LinqUtils.selectMany(getList(0), func);
    }
    @Override
    public int size() {
        try {
            return getList(0).size();
        } catch (Exception ex) {
            throw new JSException(ex, "Get size failed");
        }
    }
    @Override
    public boolean isEmpty() { return size() == 0; }
    // List methods
    @Override
    public boolean contains(Object o) {
        return getList(0).contains(o);
    }
    @Override
    public Iterator<T> iterator() {
        return getList(0).iterator();
    }
    @Override
    public Object[] toArray() {
        return getList(0).toArray();
    }
    @Override
    public <T1> T1[] toArray(T1[] a) {
        return getList(0).toArray(a);
    }
    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean containsAll(Collection<?> c) {
        return getList(c.size()).containsAll(c);
    }
    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() { }

    @Override
    public T get(int index) {
        return core().get(index).getEntity(dataClass);
    }
    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }
    @Override
    public T remove(int index) {
        return getList(index - 1).remove(index);
    }
    @Override
    public int indexOf(Object o) {
        return getList(0).indexOf(o);
    }
    @Override
    public int lastIndexOf(Object o) {
        return getList(0).lastIndexOf(o);
    }
    @Override
    public ListIterator<T> listIterator() {
        return getList(0).listIterator();
    }
    @Override
    public ListIterator<T> listIterator(int index) {
        return getList(0).listIterator(index);
    }
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return slice(fromIndex, toIndex);
    }

    public List<T> getList(int minAmount) {
        List<T> list;
        Timer timer = new Timer();
        do {
            list = core().getEntityList(dataClass);
        } while (list.size() < minAmount && timer.isRunning());
        if (list.size() < minAmount) {
            throw new JSException("Failed to get list '%s' in %s seconds", getName(), new DecimalFormat("#.##").format(timer.timePassedInMSec() / 1000));
        }
        return list;
    }
    public JS core() { return core; }
    public void setCore(JS core) { this.core = core; }
    public void setup(Field field) {
        Type[] types = getGenericTypes(field);
        if (types.length != 1)
            return;
        try {
            dataClass = types[0].toString().equals("?") ? null : (Class<T>) types[0];
        } catch (Exception ex) {
            throw new JSException(ex, "Can't get DataTable '%s' data or entity class", getName());
        }
    }

    public String getName() {
        return core().getName();
    }
    public void setName(String name) {
        core().setName(name);
    }
}