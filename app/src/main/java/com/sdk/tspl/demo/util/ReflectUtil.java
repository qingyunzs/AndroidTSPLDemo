package com.sdk.tspl.demo.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil {
    /**
     * 判断对象是否包含指定属性
     *
     * @param obj
     * @param propertyName
     * @return
     */
    public static boolean checkPropertyIsInObject(Object obj, String propertyName) {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(propertyName)) {
                return true;
            }
        }
        return false;
    }
    /**
     * 根据属性名动态调用 set 方法
     *
     * @param obj
     * @param propertyName
     * @param value
     * @throws Exception
     */
    public static void setProperty(Object obj, String propertyName, Object value) throws Exception {
        try {
            // 获取属性的 setter 方法
            String setterMethodName = "set" + capitalize(propertyName);
            Method setterMethod = obj.getClass().getMethod(setterMethodName, value.getClass());

            // 调用 setter 方法
            setterMethod.invoke(obj, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据属性名动态调用 get 方法
     *
     * @param obj
     * @param propertyName
     * @return
     * @throws Exception
     */
    public static Object getProperty(Object obj, String propertyName) {
        try {
            // 获取属性的 getter 方法
            String getterMethodName = "get" + capitalize(propertyName);
            Method getterMethod = obj.getClass().getMethod(getterMethodName);

            // 调用 getter 方法并返回结果
            return getterMethod.invoke(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 用于首字母大写，以符合 Java 方法命名规范
     *
     * @param propertyName
     * @return
     */
    private static String capitalize(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }
}
