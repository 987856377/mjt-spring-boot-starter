package com.springboot.mjt.processor;

import com.google.auto.service.AutoService;
import com.springboot.mjt.annotation.MjtMapper;
import com.springboot.mjt.annotation.MjtMapperScan;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.processor
 * @Author Xu Zhenkui
 * @Date 2023-01-06 10:10
 */
@SupportedAnnotationTypes({MjtMapperProcessor.MJTMAPPER_ANNOTATION, MjtMapperProcessor.MJTMAPPERSCAN_ANNOTATION})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class MjtMapperProcessor extends AbstractProcessor {
    public static final String MJTMAPPER_ANNOTATION = "com.springboot.mjt.annotation.MjtMapper";
    public static final String MJTMAPPERSCAN_ANNOTATION = "com.springboot.mjt.annotation.MjtMapperScan";
    private static Set<String> SCANNED_MAPPER_SET = new HashSet<>();

    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private ProcessingEnvironment processingEnv;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        processingEnv = jbUnwrap(ProcessingEnvironment.class, processingEnv);
        super.init(processingEnv);
        this.processingEnv = processingEnv;
        this.javacTrees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
    }

    private static <T> T jbUnwrap(Class<? extends T> iface, T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
        } catch (Throwable ignored) {
        }
        return unwrapped != null ? unwrapped : wrapper;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processMjtMapperScan(roundEnv);
        processMjtMapper(roundEnv);
        return true;
    }

    private void processMjtMapperScan(RoundEnvironment roundEnv) {
        Set<? extends Element> mappersSet = roundEnv.getElementsAnnotatedWith(MjtMapperScan.class);
        mappersSet.forEach(mappersPkg -> processMjtMapperScanAnnotation(roundEnv, mappersPkg));
    }

    private void processMjtMapper(RoundEnvironment roundEnv) {
        Set<? extends Element> mapperSet = roundEnv.getElementsAnnotatedWith(MjtMapper.class);
        mapperSet.forEach(mapper -> {
            if (!SCANNED_MAPPER_SET.contains(mapper.toString())) {
                processMemberValue(mapper);
            }
        });
    }

    private void processMjtMapperScanAnnotation(RoundEnvironment roundEnv, Element mappersPkg) {
        JCTree jcTree = javacTrees.getTree(mappersPkg);
        jcTree.accept(new TreeTranslator() {
            @Override
            public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                jcClassDecl.mods.annotations.forEach(annotation -> {
                    if (MJTMAPPERSCAN_ANNOTATION.equals(annotation.type.toString())) {
                        if (annotation.args.size() > 0) {
                            String[] packages = getMapperPackages(annotation.args.get(0));
                            for (String pkg : packages) {
                                Pattern compile = Pattern.compile(pkg);
                                roundEnv.getRootElements().stream()
                                        .filter((Predicate<Element>) element -> compile.matcher(element.toString()).find())
                                        .forEach(mapper -> {
                                            processMemberValue(mapper);
                                            SCANNED_MAPPER_SET.add(mapper.toString());
                                        });
                            }
                        }
                    }
                });
            }
        });
    }

    private void processMemberValue(Element mapper) {
        JCTree jcTree = javacTrees.getTree(mapper);
        jcTree.accept(new TreeTranslator() {
            @Override
            public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                jcClassDecl.defs.stream()
                        // 过滤，只处理变量类型
                        .filter(item -> item.getKind().equals(Tree.Kind.VARIABLE))
                        // 类型强转
                        .map(jcVariableDecl -> (JCTree.JCVariableDecl) jcVariableDecl).forEach(jcVariableDecl -> {
                            String varType = jcVariableDecl.vartype.type.toString();
                            if (!"java.lang.String".equals(varType)) {
                                // 限定变量类型必须是String类型，否则抛异常
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Type '" + varType + "'" + " is not support.");
                            }
                            jcVariableDecl.init = treeMaker.Literal(mapper.toString() + "." + jcVariableDecl.name.toString());
                        });
            }
        });
        System.out.println("-----> MJT mapper '" + mapper + "' processed.");
    }

    private static String[] getMapperPackages(JCTree.JCExpression expression) {
        String[] packages = {};
        if (expression.toString().contains("=")) {
            String value = expression.toString()
                    .replaceAll(" ", "")
                    .replaceAll("}", "")
                    .replaceAll("\\{", "")
                    .replaceAll("\"", "");
            packages = value.substring(value.indexOf("=") + 1).split(",");
        }
        return packages;
    }

}
