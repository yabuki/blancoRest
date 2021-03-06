/*
 * blanco Framework
 * Copyright (C) 2004-2006 IGA Tosiki
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */
package blanco.rest;

import blanco.cg.BlancoCgObjectFactory;
import blanco.cg.BlancoCgSupportedLang;
import blanco.cg.transformer.BlancoCgTransformerFactory;
import blanco.cg.util.BlancoCgLineUtil;
import blanco.cg.valueobject.*;
import blanco.commons.util.BlancoNameAdjuster;
import blanco.commons.util.BlancoStringUtil;
import blanco.rest.resourcebundle.BlancoRestResourceBundle;
import blanco.rest.valueobject.BlancoRestTelegram;
import blanco.rest.valueobject.BlancoRestTelegramField;
import blanco.rest.valueobject.BlancoRestTelegramProcess;
import blanco.valueobject.valueobject.BlancoValueObjectClassStructure;
import blanco.xml.bind.BlancoXmlBindingUtil;
import blanco.xml.bind.BlancoXmlUnmarshaller;
import blanco.xml.bind.valueobject.BlancoXmlAttribute;
import blanco.xml.bind.valueobject.BlancoXmlDocument;
import blanco.xml.bind.valueobject.BlancoXmlElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 「メッセージ定義書」Excel様式からメッセージを処理するクラス・ソースコードを生成。
 * 
 * このクラスは、中間XMLファイルからソースコードを自動生成する機能を担います。
 * 
 * @author IGA Tosiki
 * @author tueda
 */
public class BlancoRestXml2SourceFile {
    /**
     * このプロダクトのリソースバンドルへのアクセスオブジェクト。
     */
    private final BlancoRestResourceBundle fBundle = new BlancoRestResourceBundle();

    /**
     * 出力対象となるプログラミング言語。
     */
    private int fTargetLang = BlancoCgSupportedLang.JAVA;

    /**
     * 入力シートに期待するプログラミング言語
     */
    private int fSheetLang = BlancoCgSupportedLang.JAVA;

    public void setSheetLang(final int argSheetLang) {
        fSheetLang = argSheetLang;
    }

    /**
     * 内部的に利用するblancoCg用ファクトリ。
     */
    private BlancoCgObjectFactory fCgFactory = null;

    /**
     * 内部的に利用するblancoCg用ソースファイル情報。
     */
    private BlancoCgSourceFile fCgSourceFile = null;

    /**
     * 内部的に利用するblancoCg用クラス情報。
     */
    private BlancoCgClass fCgClass = null;

    /**
     * フィールド名やメソッド名の名前変形を行うかどうか。
     */
    private boolean fNameAdjust = true;

    /**
     * 要求電文のベースクラス
     */
    private String inputTelegramBase = null;
    /**
     * 応答電文のベースクラス
     */
    private String outputTelegramBase = null;

    /**
     * 自動生成するソースファイルの文字エンコーディング。
     */
    private String fEncoding = null;

    public void setEncoding(final String argEncoding) {
        fEncoding = argEncoding;
    }

    /**
     * 中間XMLファイルからソースコードを自動生成します。
     * 
     * @param argMetaXmlSourceFile
     *            メタ情報が含まれているXMLファイル。
     * @param argDirectoryTarget
     *            ソースコード生成先ディレクトリ (/mainを除く部分を指定します)。
     * @param argNameAdjust
     *            名前変形を行うかどうか。
     * @throws IOException
     *             入出力例外が発生した場合。
     */
    public void process(final File argMetaXmlSourceFile,
            final boolean argNameAdjust, final File argDirectoryTarget)
            throws IOException {

        System.out.println("BlancoRestXml2SourceFile#process file = " + argMetaXmlSourceFile.getName());

        fNameAdjust = argNameAdjust;

        // メタ情報を解析してバリューオブジェクトのツリーを取得します。
        final BlancoXmlDocument documentMeta = new BlancoXmlUnmarshaller()
                .unmarshal(argMetaXmlSourceFile);

        // ルートエレメントを取得します。
        final BlancoXmlElement elementRoot = BlancoXmlBindingUtil
                .getDocumentElement(documentMeta);
        if (elementRoot == null) {
            // ルートエレメントが無い場合には処理中断します。
            System.out.println("BlancoRestXmlSourceFile#process !!! NO ROOT ELEMENT !!!");
            return;
        }

        // まずは電文を生成します．
        ArrayList<BlancoRestTelegram> listTelegram = new ArrayList<>();
        processTelegram(argDirectoryTarget, elementRoot, listTelegram);

        // 次に電文処理を生成します
        processTelegramProcess(argDirectoryTarget, elementRoot, listTelegram);
    }

    private void processTelegramProcess(final File argDirectoryTarget, BlancoXmlElement elementRoot, List<BlancoRestTelegram> argListTelegrams) {
        // sheet(Excelシート)のリストを取得します。
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(elementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        for (int index = 0; index < sizeListSheet; index++) {
            // おのおののシートを処理します。
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // 共通情報を取得します。
            final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle
                            .getMeta2xmlProcessCommon());
            if (elementCommon == null) {
                // commonが無い場合には、このシートの処理をスキップします。
                System.out.println("BlancoRestXmlSourceFile#processTelegramProcess !!! NO COMMON !!!");
                continue;
            }

            final String name = BlancoXmlBindingUtil.getTextContent(
                    elementCommon, "name");

            if (BlancoStringUtil.null2Blank(name).trim().length() == 0) {
                // nameが空の場合には処理をスキップします。
                System.out.println("BlancoRestXmlSourceFile#processTelegramProcess !!! NO NAME !!!");
                continue;
            }

            System.out.println("BlancoRestXmlSourceFile#processTelegramProcess name = " + name);

            // 電文処理には一覧情報はありません

            // シートから詳細な情報を取得します。
            final BlancoRestTelegramProcess structure = parseProcessSheet(elementCommon);

            if (structure != null) {
                // メタ情報の解析結果をもとにソースコード自動生成を実行します。
                process(structure, argListTelegrams, argDirectoryTarget);
            }

        }
    }

    private BlancoRestTelegramProcess parseProcessSheet(final BlancoXmlElement argElementCommon) {

        final BlancoRestTelegramProcess structure = new BlancoRestTelegramProcess();
        structure.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        structure.setPackage(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "package"));

        if (BlancoStringUtil.null2Blank(structure.getPackage()).trim()
                .length() == 0) {
            throw new IllegalArgumentException(fBundle
                    .getXml2sourceFileErr001(structure.getName()));
        }

        if (BlancoXmlBindingUtil
                .getTextContent(argElementCommon, "description") != null) {
            structure.setDescription(BlancoXmlBindingUtil
                    .getTextContent(argElementCommon, "description"));
        }

        /*
         * 入力シートが Java 以外の場合にも対応します．
         * 現時点では PHP のみです．
         */
        String telegramRequestId = BlancoXmlBindingUtil.getTextContent(argElementCommon, "telegramRequestId");
        String telegramResponseId = BlancoXmlBindingUtil.getTextContent(argElementCommon, "telegramResponseId");
        switch (fSheetLang) {
            case BlancoCgSupportedLang.PHP:
                telegramRequestId = adjustClassNamePhp2Java(telegramRequestId);
                telegramResponseId = adjustClassNamePhp2Java(telegramResponseId);
                break;
            /* 対応言語を増やす場合はここに case を追記します */
        }
        structure.setRequestId(telegramRequestId);
        structure.setResponseId(telegramResponseId);

        structure.setLocation(BlancoXmlBindingUtil.getTextContent(argElementCommon, "location"));

        structure.setNamespace(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "namespace"));

        String strNoAuthenticationRequired = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "noAuthentication");
        // System.out.println("#### noAuth = " + strNoAuthenticationRequired);
        structure.setNoAuthentication("YES".equalsIgnoreCase(strNoAuthenticationRequired));

        return structure;
    }

    private void processTelegram(final File argDirectoryTarget, BlancoXmlElement elementRoot, List<BlancoRestTelegram> argListTelegrams) {

        // sheet(Excelシート)のリストを取得します。
        final List<BlancoXmlElement> listSheet = BlancoXmlBindingUtil
                .getElementsByTagName(elementRoot, "sheet");
        final int sizeListSheet = listSheet.size();
        for (int index = 0; index < sizeListSheet; index++) {
            // おのおののシートを処理します。
            final BlancoXmlElement elementSheet = (BlancoXmlElement) listSheet
                    .get(index);

            // 共通情報を取得します。
            final BlancoXmlElement elementCommon = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle
                            .getMeta2xmlTelegramCommon());
            if (elementCommon == null) {
                // commonが無い場合には、このシートの処理をスキップします。
                // System.out.println("BlancoRestXmlSourceFile#process !!! NO COMMON !!!");
                continue;
            }

            final String name = BlancoXmlBindingUtil.getTextContent(
                    elementCommon, "name");

            if (BlancoStringUtil.null2Blank(name).trim().length() == 0) {
                // nameが空の場合には処理をスキップします。
                // System.out.println("BlancoRestXmlSourceFile#process !!! NO NAME !!!");
                continue;
            }

            System.out.println("/* tueda */ BlancoRestXmlSourceFile#process name = " + name);

            // 一覧情報を取得します。
            final BlancoXmlElement elementList = BlancoXmlBindingUtil
                    .getElement(elementSheet, fBundle.getMeta2xmlTeregramList());

            // シートから詳細な情報を取得します。
            final BlancoRestTelegram processTelegram = parseTelegramSheet(
                    elementCommon, elementList);

            if (processTelegram != null) {
                // メタ情報の解析結果をもとにソースコード自動生成を実行します。
                process(processTelegram, argDirectoryTarget);
                argListTelegrams.add(processTelegram);
            }
        }
    }

    /**
     * sheetエレメントを展開します。
     * 
     * @param argElementCommon
     *            現在処理しているCommonノード。
     * @param argElementList
     *            現在処理しているListノード。
     * @return 収集されたメタ情報構造データ。
     */
    private BlancoRestTelegram parseTelegramSheet(
            final BlancoXmlElement argElementCommon,
            final BlancoXmlElement argElementList) {

        final BlancoRestTelegram processTelegram = new BlancoRestTelegram();
        processTelegram.setName(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "name"));
        processTelegram.setPackage(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "package"));

        if (BlancoStringUtil.null2Blank(processTelegram.getPackage()).trim()
                .length() == 0) {
            throw new IllegalArgumentException(fBundle
                    .getXml2sourceFileErr001(processTelegram.getName()));
        }

        if (BlancoXmlBindingUtil
                .getTextContent(argElementCommon, "description") != null) {
            processTelegram.setDescription(BlancoXmlBindingUtil
                    .getTextContent(argElementCommon, "description"));
        }

        processTelegram.setNamespace(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "namespace"));

        processTelegram.setTelegramType(BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "type"));

        String superClass = BlancoXmlBindingUtil.getTextContent(
                argElementCommon, "superClass");

        /*
         * 入力シートが Java 以外の場合にも対応します．
         * 現時点では PHP のみです．
         */
        switch (fSheetLang) {
            case BlancoCgSupportedLang.PHP:
                superClass = adjustClassNamePhp2Java(superClass);
                break;
            /* 対応言語を増やす場合はここに case を追記します */
        }

        processTelegram.setTelegramSuperClass(superClass);

        if (argElementList == null) {
            return null;
        }

        // 一覧の内容を取得します。
        final List<BlancoXmlElement> listField = BlancoXmlBindingUtil
                .getElementsByTagName(argElementList, "field");
        for (int indexField = 0; indexField < listField.size(); indexField++) {
            final Object nodeField = listField.get(indexField);

            if (nodeField instanceof BlancoXmlElement == false) {
                System.out.println("BlancoRestXml2SourceFile#parseTelegramSheet: NO FIELD !!!");
                continue;
            }

            final BlancoXmlElement elementField = (BlancoXmlElement) nodeField;
            BlancoRestTelegramField field = new BlancoRestTelegramField();
            field
                    .setNo(BlancoXmlBindingUtil.getTextContent(elementField,
                            "no"));

            field.setName(BlancoXmlBindingUtil.getTextContent(elementField,
                    "fieldName"));
            if (BlancoStringUtil.null2Blank(field.getName()).length() == 0) {
                continue;
            }

//            System.out.println("BlancoRestXml2SourceFile#parseTelegramSheet: name = " + field.getName());

            // 既に同じ内容が登録されていないかどうかのチェック。
            for (int indexPast = 0; indexPast < processTelegram.getListField()
                    .size(); indexPast++) {
                final BlancoRestTelegramField fieldPast = processTelegram
                        .getListField().get(indexPast);
                if (fieldPast.getName().equals(field.getName())) {
                    throw new IllegalArgumentException(
                            fBundle.getXml2sourceFileErr003(processTelegram
                                    .getName(), field.getName()));
                }
            }

            /*
             * Java 以外のプログラミング言語用に定義された Excel シートに対応
             * 現時点では PHP のみに対応します
            */
            String fieldType = BlancoXmlBindingUtil.getTextContent(elementField,
                    "fieldType");
            if (BlancoStringUtil.null2Blank(fieldType).length() == 0) {
                // ここで異常終了。
                continue;
            }
            switch (fSheetLang) {
                case BlancoCgSupportedLang.PHP:
                    fieldType = adjustClassNamePhp2Java(fieldType);
                    break;
            }
            field.setFieldType(fieldType);

            field.setDescription(BlancoXmlBindingUtil.getTextContent(
                    elementField, "description"));

            String strFieldRequired = BlancoXmlBindingUtil.getTextContent(
                    elementField, "fieldRequired");
            field.setFieldRequired("YES".equalsIgnoreCase(strFieldRequired));

            field.setDefault(BlancoXmlBindingUtil.getTextContent(
                    elementField, "default"));

            try {
                String strMinLength = BlancoXmlBindingUtil.getTextContent(
                        elementField, "minLength");
                field.setMinLength(Integer.parseInt(strMinLength));

                String strMaxLength = BlancoXmlBindingUtil.getTextContent(
                        elementField, "maxLength");
                field.setMaxLength(Integer.parseInt(strMaxLength));
            } catch (NumberFormatException e) {
                // 値がセットされていなかったり数値でなかった場合は無視
            }

            field.setMinInclusive(BlancoXmlBindingUtil.getTextContent(
                    elementField, "minInclusive"));
            field.setMaxInclusive(BlancoXmlBindingUtil.getTextContent(
                    elementField, "maxInclusive"));

            field.setPattern(BlancoXmlBindingUtil.getTextContent(
                    elementField, "pattern"));

            field.setFieldBiko(BlancoXmlBindingUtil.getTextContent(
                    elementField, "fieldBiko"));

            processTelegram.getListField().add(field);
        }

        return processTelegram;
    }

    /**
     * 収集された情報を元に、電文処理のソースコードを自動生成します。
     *
     * @param argStructure
     *            メタファイルから収集できた処理構造データ。
     * @param argDirectoryTarget
     *            ソースコードの出力先フォルダ。
     */
    public void process(
            final BlancoRestTelegramProcess argStructure,
            final List<BlancoRestTelegram> argListTelegrams,
            final File argDirectoryTarget) {

        // 従来と互換性を持たせるため、/mainサブフォルダに出力します。
        final File fileBlancoMain = new File(argDirectoryTarget
                .getAbsolutePath()
                + "/main");

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(argStructure
                .getPackage(), "このソースコードは blanco Frameworkによって自動生成されています。");
        fCgSourceFile.setEncoding(fEncoding);
        fCgClass = fCgFactory.createClass(BlancoRestConstants.PREFIX_ABSTRACT + argStructure.getName(),
                BlancoStringUtil.null2Blank(argStructure
                        .getDescription()));
        // ApiBase クラスを継承
        BlancoCgType fCgType = new BlancoCgType();
        fCgType.setName(BlancoRestConstants.BASE_CLASS);
        fCgClass.setExtendClassList(new ArrayList<>());
        fCgClass.getExtendClassList().add(fCgType);

        // abstrac フラグをセット
        fCgClass.setAbstract(true);

        fCgSourceFile.getClassList().add(fCgClass);

        if (argStructure.getDescription() != null) {
            fCgSourceFile.setDescription(argStructure
                    .getDescription());
        }

        // API実装クラスで実装させる abstract method の定義
        createAbstractMethod(argStructure, argListTelegrams);

        // base class からの abstract method の実装
        createExecuteMethod(argStructure, argListTelegrams);

        // isAuthenticationRequired メソッドの上書き
        overrideAuthenticationRequired(argStructure);

        // RequestId 名を取得する メソッド
        createRequestIdMethod(argStructure);

        // ResponseId 名を取得する メソッド
        createResponseIdMethod(argStructure);

        // required 文を出力しない ... 将来的には xls で指定するように？
        // fCgSourceFile.setIsImport(false);

        BlancoCgTransformerFactory.getSourceTransformer(fTargetLang).transform(
                fCgSourceFile, fileBlancoMain);
    }

    private void createAbstractMethod(BlancoRestTelegramProcess argStructure, List<BlancoRestTelegram>  argListTelegrams) {

        // Initializer の定義
//        final BlancoCgMethod cgInitializerMethod = fCgFactory.createMethod(
//                BlancoRestConstants.API_INITIALIZER_METHOD, fBundle.getXml2sourceFileInitializerDescription());
//        fCgClass.getMethodList().add(cgInitializerMethod);
//        cgInitializerMethod.setAccess("protected");
//        cgInitializerMethod.setAbstract(true);
        // ApiBase で固定的に定義

        // Processor の定義
        final BlancoCgMethod cgProcessorMethod = fCgFactory.createMethod(
                BlancoRestConstants.API_PROCESS_METHOD, fBundle.getXml2sourceFileProcessorDescription());
        fCgClass.getMethodList().add(cgProcessorMethod);
        cgProcessorMethod.setAccess("protected");
        cgProcessorMethod.setAbstract(true);

        String requestId = argStructure.getRequestId();
        String requestSubId = requestId;
        String responseId = argStructure.getResponseId();
        String responseSubId = responseId;

        /*
         * Java 版では引数の型は厳密に指定します．
         */
//        for (BlancoRestTelegram telegram : argListTelegrams) {
////            System.out.println("### type = " + telegram.getTelegramType());
//            if ("Input".equals(telegram.getTelegramType())) {
//                requestId = telegram.getTelegramSuperClass();
//            }
//            if ("Output".equals(telegram.getTelegramType())) {
//                responseId = telegram.getTelegramSuperClass();
//            }
//        }
        for (BlancoRestTelegram telegram : argListTelegrams) {
//            System.out.println("### type = " + telegram.getTelegramType());
            if ("Input".equals(telegram.getTelegramType())) {
                requestId = telegram.getPackage() + "." + requestId;
            }
            if ("Output".equals(telegram.getTelegramType())) {
                responseId = telegram.getPackage() + "." + responseId;
            }
        }

        cgProcessorMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + requestSubId, requestId,
                        fBundle.getXml2sourceFileProsessorArgLangdoc()));

        cgProcessorMethod.setReturn(fCgFactory.createReturn(responseId,
                fBundle.getXml2sourceFileProsessorReturnLangdoc()));

    }

    private void createExecuteMethod(BlancoRestTelegramProcess argStructure, List<BlancoRestTelegram>  argListTelegrams) {
        final BlancoCgMethod cgExecutorMethod = fCgFactory.createMethod(
                BlancoRestConstants.BASE_EXECUTOR_METHOD, fBundle.getXml2sourceFileExecutorDescription());
        fCgClass.getMethodList().add(cgExecutorMethod);
        cgExecutorMethod.setAccess("protected");

        /*
         * 型チェックを通す為にSuperClassがある場合はそれを使います
         */
        String requestId = argStructure.getRequestId();
        String requestSubId = requestId;
        String responseId = argStructure.getResponseId();
        String responseSubId = responseId;

        /*
         * Java 版では引数の型は厳密に指定します．
         */
//        for (BlancoRestTelegram telegram : argListTelegrams) {
////            System.out.println("### type = " + telegram.getTelegramType());
//            if ("Input".equals(telegram.getTelegramType())) {
//                requestId = telegram.getTelegramSuperClass();
//            }
//            if ("Output".equals(telegram.getTelegramType())) {
//                responseId = telegram.getTelegramSuperClass();
//            }
//        }
        for (BlancoRestTelegram telegram : argListTelegrams) {
//            System.out.println("### type = " + telegram.getTelegramType());
            if ("Input".equals(telegram.getTelegramType())) {
                requestId = telegram.getPackage() + "." + requestId;
            }
            if ("Output".equals(telegram.getTelegramType())) {
                responseId = telegram.getPackage() + "." + responseId;
            }
        }

        cgExecutorMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + requestSubId, requestId,
                        fBundle
                                .getXml2sourceFileExecutorArgLangdoc()));

        cgExecutorMethod.setReturn(fCgFactory.createReturn(responseId,
                fBundle.getXml2sourceFileExecutorReturnLangdoc()));

        // メソッドの実装
        final List<String> listLine = cgExecutorMethod.getLineList();

        listLine.add(
                responseId + " " + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "ret" + responseSubId + " = "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "this." + BlancoRestConstants.API_PROCESS_METHOD
                        + "( " + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "arg" + requestSubId + " )"
                        + BlancoCgLineUtil.getTerminator(fTargetLang));

        listLine.add("\n");
        listLine.add("return "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "ret" + responseSubId
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void overrideAuthenticationRequired(BlancoRestTelegramProcess argStructure) {
        String methodName = BlancoRestConstants.API_AUTHENTICATION_REQUIRED;

        final BlancoCgMethod cgAuthenticationRequiredMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileAuthflagDescription());
        fCgClass.getMethodList().add(cgAuthenticationRequiredMethod);
        cgAuthenticationRequiredMethod.setAccess("protected");

        cgAuthenticationRequiredMethod.setReturn(fCgFactory.createReturn("java.lang.Boolean",
                fBundle.getXml2sourceFileAuthflagReturnLangdoc()));

        // メソッドの実装
        final List<String> listLine = cgAuthenticationRequiredMethod.getLineList();

        String retval = "true";
        if (argStructure.getNoAuthentication()) {
            retval = "false";
        }

        listLine.add("return " + retval
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void createRequestIdMethod(BlancoRestTelegramProcess argStructure) {
        String methodName = BlancoRestConstants.API_REQUESTID_METHOD;

        final BlancoCgMethod cgRequestIdMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileRequestidDesctiption());
        fCgClass.getMethodList().add(cgRequestIdMethod);
        cgRequestIdMethod.setAccess("protected");

        List<String> annotators = new ArrayList<>();
        annotators.add("Override");
        cgRequestIdMethod.setAnnotationList(annotators);

        cgRequestIdMethod.setReturn(fCgFactory.createReturn("java.lang.String",
                fBundle.getXml2sourceFileRequestidReturnLangdoc()));

        // メソッドの実装
        final List<String> listLine = cgRequestIdMethod.getLineList();


        listLine.add("return " + "\"" + argStructure.getPackage() + "." + argStructure.getRequestId() + "\""
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    private void createResponseIdMethod(BlancoRestTelegramProcess argStructure) {
        String methodName = BlancoRestConstants.API_RESPONSE_METHOD;

        final BlancoCgMethod cgResponseIdMethod = fCgFactory.createMethod(
                methodName, fBundle.getXml2sourceFileRequestidDesctiption());
        fCgClass.getMethodList().add(cgResponseIdMethod);
        cgResponseIdMethod.setAccess("protected");

        List<String> annotators = new ArrayList<>();
        annotators.add("Override");
        cgResponseIdMethod.setAnnotationList(annotators);

        cgResponseIdMethod.setReturn(fCgFactory.createReturn("java.lang.String",
                fBundle.getXml2sourceFileRequestidReturnLangdoc()));

        // メソッドの実装
        final List<String> listLine = cgResponseIdMethod.getLineList();


        listLine.add("return " + "\"" + argStructure.getPackage() + "." + argStructure.getResponseId() + "\""
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * 収集された情報を元に、ソースコードを自動生成します。
     * 
     * @param argStructure
     *            メタファイルから収集できた処理構造データ。
     * @param argDirectoryTarget
     *            ソースコードの出力先フォルダ。
     */
    public void process(
            final BlancoRestTelegram argStructure,
            final File argDirectoryTarget) {

        // 従来と互換性を持たせるため、/mainサブフォルダに出力します。
        final File fileBlancoMain = new File(argDirectoryTarget
                .getAbsolutePath()
                + "/main");

        fCgFactory = BlancoCgObjectFactory.getInstance();
        fCgSourceFile = fCgFactory.createSourceFile(argStructure
                .getPackage(), "このソースコードは blanco Frameworkによって自動生成されています。");
        fCgSourceFile.setEncoding(fEncoding);
        fCgClass = fCgFactory.createClass(argStructure.getName(),
                BlancoStringUtil.null2Blank(argStructure
                        .getDescription()));

        // ApiTelegram クラスを継承
        String telegramBase = argStructure.getTelegramSuperClass();
        if (telegramBase != null) {

            BlancoValueObjectClassStructure objectClassStructure =
                    BlancoRestObjectsInfo.objects.get(telegramBase);

            String packageName = null;
            if (objectClassStructure != null && (packageName = objectClassStructure.getPackage()) != null) {
                telegramBase = packageName + "." + telegramBase;
            }

            BlancoCgType fCgType = new BlancoCgType();
            fCgType.setName(telegramBase);

            fCgClass.setExtendClassList(new ArrayList<>());
            fCgClass.getExtendClassList().add(fCgType);

        }

        fCgSourceFile.getClassList().add(fCgClass);

        if (argStructure.getDescription() != null) {
            fCgSourceFile.setDescription(argStructure
                    .getDescription());
        }

        expandValueObject(argStructure);

        // required 文を出力しない ... 将来的には xls で指定するように？
        fCgSourceFile.setIsImport(false);

        BlancoCgTransformerFactory.getSourceTransformer(fTargetLang).transform(
                fCgSourceFile, fileBlancoMain);
    }

    /**
     * バリューオブジェクトを展開します。
     * 
     * @param argProcessStructure
     *            メタファイルから収集できた処理構造データ。
     */
    private void expandValueObject(
            final BlancoRestTelegram argProcessStructure) {

        for (int indexField = 0; indexField < argProcessStructure
                .getListField().size(); indexField++) {
            final BlancoRestTelegramField fieldLook = argProcessStructure
                    .getListField().get(indexField);

            expandField(argProcessStructure, fieldLook);

            expandMethodSet(argProcessStructure, fieldLook);

            expandMethodGet(argProcessStructure, fieldLook);

            expandMethodType(argProcessStructure, fieldLook);
        }

        expandMethodToString(argProcessStructure);
    }

    /**
     * フィールドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandField(
            final BlancoRestTelegram argProcessStructure,
            final BlancoRestTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgField cgField = fCgFactory.createField("f" + fieldName,
                fieldLook.getFieldType(), "");
        fCgClass.getFieldList().add(cgField);
        cgField.setAccess("private");

        cgField.setDescription(fBundle.getXml2sourceFileFieldName(fieldLook
                .getName()));
        cgField.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileFieldType(fieldLook.getFieldType()));
        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgField.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }
    }

    /**
     * setメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodSet(
            final BlancoRestTelegram argProcessStructure,
            final BlancoRestTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("set"
                + fieldName, fBundle.getXml2sourceFileSetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileSetLangdoc02(fieldLook.getFieldType()));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        cgMethod.getParameterList().add(
                fCgFactory.createParameter("arg" + fieldName, fieldLook
                        .getFieldType(), fBundle
                        .getXml2sourceFileSetArgLangdoc(fieldLook.getName())));

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                + "this.f" + fieldName + " = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "arg"
                + fieldName + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * getメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodGet(
            final BlancoRestTelegram argProcessStructure,
            final BlancoRestTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("get"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileGetLangdoc02(fieldLook.getFieldType()));

        cgMethod.setReturn(fCgFactory.createReturn(fieldLook.getFieldType(), fBundle
                .getXml2sourceFileGetReturnLangdoc(fieldLook.getName())));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "this." + "f" + fieldName
                        + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * typeメソッドを展開します
     *
     * @param argProcessStructure
     * @param fieldLook
     */
    private void expandMethodType(
            final BlancoRestTelegram argProcessStructure,
            final BlancoRestTelegramField fieldLook) {
        String fieldName = fieldLook.getName();
        if (fNameAdjust) {
            fieldName = BlancoNameAdjuster.toClassName(fieldName);
        }

        final BlancoCgMethod cgMethod = fCgFactory.createMethod("type"
                + fieldName, fBundle.getXml2sourceFileGetLangdoc01(fieldLook
                .getName()));
        fCgClass.getMethodList().add(cgMethod);
        cgMethod.setAccess("public");
        cgMethod.setStatic(true);

        cgMethod.getLangDoc().getDescriptionList().add(
                fBundle.getXml2sourceFileTypeLangdoc02("java.lang.String"));

        cgMethod.setReturn(fCgFactory.createReturn("java.lang.String", fBundle
                .getXml2sourceFileTypeReturnLangdoc(fieldLook.getName())));

        if (BlancoStringUtil.null2Blank(fieldLook.getDescription()).length() > 0) {
            cgMethod.getLangDoc().getDescriptionList().add(
                    fieldLook.getDescription());
        }

        // メソッドの実装
        final List<String> listLine = cgMethod.getLineList();

        listLine
                .add("return " +
                        BlancoCgLineUtil.getStringLiteralEnclosure(BlancoCgSupportedLang.JAVA) +
                                fieldLook.getFieldType() +
                                BlancoCgLineUtil.getStringLiteralEnclosure(BlancoCgSupportedLang.JAVA) +
                                BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * toStringメソッドを展開します。
     * 
     * @param argProcessStructure
     */
    private void expandMethodToString(
            final BlancoRestTelegram argProcessStructure) {
        final BlancoCgMethod method = fCgFactory.createMethod("toString",
                "このバリューオブジェクトの文字列表現を取得します。");
        fCgClass.getMethodList().add(method);

        method.getLangDoc().getDescriptionList().add(
                "オブジェクトのシャロー範囲でしかtoStringされない点に注意して利用してください。");
        method
                .setReturn(fCgFactory.createReturn("java.lang.String",
                        "バリューオブジェクトの文字列表現。"));

        List<String> annotators = new ArrayList<>();
        annotators.add("Override");
        method.setAnnotationList(annotators);

        final List<String> listLine = method.getLineList();

        listLine.add(BlancoCgLineUtil.getVariableDeclaration(fTargetLang,
                "buf", "java.lang.String", BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang)
                        + BlancoCgLineUtil
                        .getStringLiteralEnclosure(fTargetLang))
                + BlancoCgLineUtil.getTerminator(fTargetLang));

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf "
                + BlancoCgLineUtil.getStringConcatenationOperator(fTargetLang)
                + " " + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + argProcessStructure.getPackage() + "."
                + argProcessStructure.getName() + "["
                + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + BlancoCgLineUtil.getTerminator(fTargetLang));
        for (int indexField = 0; indexField < argProcessStructure
                .getListField().size(); indexField++) {
            final BlancoRestTelegramField fieldLook = argProcessStructure
                    .getListField().get(indexField);

            String fieldName = fieldLook.getName();
            if (fNameAdjust) {
                fieldName = BlancoNameAdjuster.toClassName(fieldName);
            }

            if (fieldLook.getFieldType().equals("array") == false) {
                String strLine = BlancoCgLineUtil
                        .getVariablePrefix(fTargetLang)
                        + "buf = "
                        + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                        + "buf "
                        + BlancoCgLineUtil
                                .getStringConcatenationOperator(fTargetLang)
                        + " "
                        + BlancoCgLineUtil
                                .getStringLiteralEnclosure(fTargetLang)
                        + (indexField == 0 ? "" : ",")
                        + fieldLook.getName()
                        + "="
                        + BlancoCgLineUtil
                                .getStringLiteralEnclosure(fTargetLang)
                        + " "
                        + BlancoCgLineUtil
                                .getStringConcatenationOperator(fTargetLang)
                        + " ";
                if (fieldLook.getFieldType().equals("java.lang.String")) {
                    strLine += BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this.f" + fieldName;
                } else if (fieldLook.getFieldType().equals("boolean")) {
                    strLine += "("
                            + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this.f" + fieldName + " ? 'true' : 'false')";
                } else {
                    strLine += " "
                            + BlancoCgLineUtil.getVariablePrefix(fTargetLang)
                            + "this.f" + fieldName;
                }
                strLine += BlancoCgLineUtil.getTerminator(fTargetLang);
                listLine.add(strLine);
            } else {
                listLine.add("// TODO 配列は未対応です。");
            }
        }

        listLine.add(BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf = "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf "
                + BlancoCgLineUtil.getStringConcatenationOperator(fTargetLang)
                + " " + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + "]" + BlancoCgLineUtil.getStringLiteralEnclosure(fTargetLang)
                + BlancoCgLineUtil.getTerminator(fTargetLang));
        listLine.add("return "
                + BlancoCgLineUtil.getVariablePrefix(fTargetLang) + "buf"
                + BlancoCgLineUtil.getTerminator(fTargetLang));
    }

    /**
     * PHP 用に作成されたExcelシートに定義されたクラス名にパッケージ名を付加します
     * @param phpType
     * @return
     */
    private String adjustClassNamePhp2Java(String phpType) {
                /*
                 * 型の取得．ここで Java 風の型名に変えておく
                 */

        System.out.println("/* tueda */ adjustClassNamePhp2Java: " + phpType);
        String javaType = phpType;
        if ("boolean".equalsIgnoreCase(phpType)) {
            javaType = "java.lang.Boolean";
        } else
        if ("integer".equalsIgnoreCase(phpType)) {
            javaType = "java.lang.Integer";
        } else
        if ("double".equalsIgnoreCase(phpType)) {
            javaType = "java.lang.Double";
        } else
        if ("float".equalsIgnoreCase(phpType)) {
            javaType = "java.lang.Double";
        } else
        if ("string".equalsIgnoreCase(phpType)) {
            javaType = "java.lang.String";
        } else
        if ("array".equalsIgnoreCase(phpType)) {
            javaType = "java.util.ArrayList<?>";
        } else
        if ("object".equalsIgnoreCase(phpType)) {
            javaType = "java.lang.Object";
        } else {
                    /* この名前の package を探す */
            BlancoValueObjectClassStructure structure = BlancoRestObjectsInfo.objects.get(phpType);
            if (structure != null) {
                String packageName = structure.getPackage();
                if (packageName != null) {
                    javaType = packageName + "." + phpType;
                }
            }
                    /* その他はそのまま記述する */
            System.out.println("/* tueda */ Unknown php type: " + javaType);
        }
        System.out.println("/* tueda */ adjustClassNamePhp2Java: " + javaType);

        return javaType;
    }
}
