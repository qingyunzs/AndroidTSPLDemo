package com.sdk.tspl.demo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import com.google.gson.Gson;

import tspl.HPRTPrinterHelper;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private PendingIntent mPermissionIntent = null;
    private UsbDevice device = null;
    private UsbManager mUsbManager = null;
    private String ConnectType = "";
    private static final String ACTION_USB_PERMISSION = "com.HPRTSDKSample";

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.sdk.tspl.demo", appContext.getPackageName());

        AfsPrintContent content = new AfsPrintContent();
        content.setProductValue("香菇烧冬瓜");
        content.setQuantityValue("136公斤");
        content.setProductionDateValue("4天");
        content.setStorageConditionsValue("冷藏0-5°C□ 冷冻-18°C□");
        content.setRemarkValue("备注。。。");
        content.setBarCodeValue("S1234567890");
        int styleVersion = 2;

        List<PrintTemplateData> templateDataList = new ArrayList<>();
        List<String> templateData = readAssetFile(appContext, "AFS_PRINT_TEMPLATE_" + styleVersion + ".txt");
        for (String line : templateData) {
            String[] lineData = line.split(" ");
            String printCmd = lineData[0];
            String printData = lineData[1];
            String[] printDataArray = printData.split(",");

            // 数据绑定
            String lastElement = printDataArray[printDataArray.length - 1];
            if (lastElement.contains("{")) {
                // 获取指定数据
                String propertyName = lastElement.substring(1, lastElement.length() - 1);
                Object propertyValue = getProperty(content, propertyName);
                printDataArray[printDataArray.length - 1] = (String) propertyValue;
            }

            // 组装数据
             // 组装数据
            List<Object> newLineData = new ArrayList<>();
            for (int i = 0; i < printDataArray.length; i++) {
                if (printCmd.equals("printText") && i == (printDataArray.length - 2)) {
                    newLineData.add(Integer.parseInt(printDataArray[i]));
                    continue;
                }
                newLineData.add(printDataArray[i]);
            }
            PrintTemplateData printTemplateData = new PrintTemplateData();
            printTemplateData.setPrintCmd(printCmd);
            printTemplateData.setParams(newLineData);
            templateDataList.add(printTemplateData);
        }
        Gson gson = new Gson();
        Log.w("打印：", gson.toJson(templateDataList));

        try {
            init(appContext);
            createUsbConnect(appContext);

            Thread.sleep(500);

            HPRTPrinterHelper.isWriteLog = true;
            HPRTPrinterHelper.isHex = true;
            HPRTPrinterHelper.printAreaSize("55", "40");
            HPRTPrinterHelper.CLS(); // 清除打印缓冲区内容
            HPRTPrinterHelper.Density("5"); // 打印浓度

            for (PrintTemplateData printTemplateData : templateDataList) {
                String methodName = printTemplateData.getPrintCmd();
                List<Object> params = printTemplateData.getParams();
                Log.i("打印cmd：", methodName + " " + gson.toJson(params));

                Class<?>[] parameterTypes = new Class<?>[params.size()];
                for (int i = 0; i < params.size(); i++) {
                    if (params.get(i) instanceof Integer) {
                        parameterTypes[i] = int.class;
                    } else {
                        parameterTypes[i] = String.class;
                    }
                }
//                Log.i("打印cmd类型", gson.toJson(parameterTypes));
                Method method = HPRTPrinterHelper.class.getMethod(methodName, parameterTypes);
                method.invoke(null, params.toArray());


//                Method method;
//                switch (methodName) {
//                    case "Box":
////                        method = HPRTPrinterHelper.class.getMethod(methodName, String.class, String.class, String.class, String.class, String.class);
//                        method.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4));
//                        break;
//                    case "printText":
////                        method = HPRTPrinterHelper.class.getMethod(methodName, String.class, String.class, String.class, String.class, String.class, String.class, int.class, String.class);
//                        method.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5), params.get(6), params.get(7));
//                        break;
//                    case "printBarcode":
////                        method = HPRTPrinterHelper.class.getMethod(methodName, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class);
//                        method.invoke(null, params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), params.get(5), params.get(6), params.get(7), params.get(8));
//                        break;
//                }
            }

            HPRTPrinterHelper.Print("1","1");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PRINT", e.getMessage());
        }
    }

    private void init(Context context) {
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mUsbReceiver, filter);
    }

    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.w("广播：", action);
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (HPRTPrinterHelper.PortOpen(context, device) != 0) {
//				        		HPRTPrinter=null;
//                                txtTips.setText(thisCon.getString(R.string.activity_main_connecterr));
                                return;
                            } else {
//                                txtTips.setText(thisCon.getString(R.string.activity_main_connected));
                                Log.w("广播：", "connect success.");
                            }
                        } else {
                            return;
                        }
                    }
                }
                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device != null) {
                        int count = device.getInterfaceCount();
                        for (int i = 0; i < count; i++) {
                            UsbInterface intf = device.getInterface(i);
                            //Class ID 7代表打印机
                            if (intf.getInterfaceClass() == 7) {
                                HPRTPrinterHelper.PortClose();
//                                txtTips.setText(R.string.activity_main_tips);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> mUsbReceiver ")).append(e.getMessage()).toString());
            }
        }
    };

    private void createUsbConnect(Context context) {
        try {

            if (null != mUsbManager) {
                return;
            }

            HPRTPrinterHelper.PortClose();
            ConnectType = "USB";
            // USB not need call "iniPort"
            mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            boolean HavePrinter = false;
            while (deviceIterator.hasNext()) {
                device = deviceIterator.next();
                int count = device.getInterfaceCount();
                for (int i = 0; i < count; i++) {
                    UsbInterface intf = device.getInterface(i);
                    if (intf.getInterfaceClass() == 7) {
                        HavePrinter = true;
                        mUsbManager.requestPermission(device, mPermissionIntent);
                        Log.d("HPRTSDKSample", "Activity_Main --> createUsbConnect --> requestPermission建立连接");
                    }
                }
            }
            if (!HavePrinter) {
//                    txtTips.setText(thisCon.getString(R.string.activity_main_connect_usb_printer));
//                Toast.makeText(thisCon, "failure", Toast.LENGTH_SHORT).show();
                Log.e("HPRTSDKSample", "Activity_Main --> createUsbConnect --> 打印机未连接");
            }
        } catch (Exception e) {
            Log.e("HPRTSDKSample", (new StringBuilder("Activity_Main --> onClickConnect " + ConnectType)).append(e.getMessage()).toString());
        }
    }

    public static List<String> readAssetFile(Context context, String fileName) {
        List<String> lines = new ArrayList<>();
        AssetManager assetManager = context.getAssets();

        try (InputStream inputStream = assetManager.open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("读取文件", "文件名：" + fileName);
        }

        return lines;
    }

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

    private static String capitalize(String propertyName) {
        return propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

    public class AfsPrintContent {
        /**
         * 标题
         */
        private String title = "云南空港航空食品有限公司";
        /**
         * 标题英文名
         */
        private String titleEn = "Yunnan International Airport Air Catering Co.,Ltd.";
        /**
         * 品名
         */
        private String productName = "品名";
        private String productNameEn = "name";
        private String productValue;
        /**
         * 数量
         */
        private String quantity = "数量";
        private String quantityEn = "Quantity";
        private String quantityValue;
        /**
         * 保质期
         */
        private String productionDate = "保质期";
        private String productionDateEn = "Production date";
        private String productionDateValue;
        /**
         * 储存条件
         */
        private String storageConditions = "储存条件";
        private String storageConditionsEn = "Storage conditions";
        private String storageConditionsValue;
        /**
         * 备注
         */
        private String remark = "备注";
        private String remarkEn = "Remark";
        private String remarkValue;
        /**
         * 条码
         */
        private String barCode = "条码";
        private String barCodeValue;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitleEn() {
            return titleEn;
        }

        public void setTitleEn(String titleEn) {
            this.titleEn = titleEn;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getProductNameEn() {
            return productNameEn;
        }

        public void setProductNameEn(String productNameEn) {
            this.productNameEn = productNameEn;
        }

        public String getProductValue() {
            return productValue;
        }

        public void setProductValue(String productValue) {
            this.productValue = productValue;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }

        public String getQuantityEn() {
            return quantityEn;
        }

        public void setQuantityEn(String quantityEn) {
            this.quantityEn = quantityEn;
        }

        public String getQuantityValue() {
            return quantityValue;
        }

        public void setQuantityValue(String quantityValue) {
            this.quantityValue = quantityValue;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionDateEn() {
            return productionDateEn;
        }

        public void setProductionDateEn(String productionDateEn) {
            this.productionDateEn = productionDateEn;
        }

        public String getProductionDateValue() {
            return productionDateValue;
        }

        public void setProductionDateValue(String productionDateValue) {
            this.productionDateValue = productionDateValue;
        }

        public String getStorageConditions() {
            return storageConditions;
        }

        public void setStorageConditions(String storageConditions) {
            this.storageConditions = storageConditions;
        }

        public String getStorageConditionsEn() {
            return storageConditionsEn;
        }

        public void setStorageConditionsEn(String storageConditionsEn) {
            this.storageConditionsEn = storageConditionsEn;
        }

        public String getStorageConditionsValue() {
            return storageConditionsValue;
        }

        public void setStorageConditionsValue(String storageConditionsValue) {
            this.storageConditionsValue = storageConditionsValue;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getRemarkEn() {
            return remarkEn;
        }

        public void setRemarkEn(String remarkEn) {
            this.remarkEn = remarkEn;
        }

        public String getRemarkValue() {
            return remarkValue;
        }

        public void setRemarkValue(String remarkValue) {
            this.remarkValue = remarkValue;
        }

        public String getBarCode() {
            return barCode;
        }

        public void setBarCode(String barCode) {
            this.barCode = barCode;
        }

        public String getBarCodeValue() {
            return barCodeValue;
        }

        public void setBarCodeValue(String barCodeValue) {
            this.barCodeValue = barCodeValue;
        }
    }

    public class PrintTemplateData {
        private String printCmd;
        private List<Object> params;

        public String getPrintCmd() {
            return printCmd;
        }

        public void setPrintCmd(String printCmd) {
            this.printCmd = printCmd;
        }

        public List<Object> getParams() {
            return params;
        }

        public void setParams(List<Object> params) {
            this.params = params;
        }
    }
}