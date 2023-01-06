package com.springboot.mjt.processor;

import com.springboot.mjt.annotation.MjtMapper;
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
import java.util.Set;

/**
 * @Description 在 resource 下创建 META-INF/services/javax.annotation.processing.Processor 文件
 * 填入 com.springboot.mjt.processor.MjtMapperProcessor
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.processor
 * @Author Xu Zhenkui
 * @Date 2023-01-06 10:10
 */
@SupportedAnnotationTypes("com.springboot.mjt.annotation.MjtMapper")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MjtMapperProcessor extends AbstractProcessor {
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
        Set<? extends Element> mapperSet = roundEnv.getElementsAnnotatedWith(MjtMapper.class);
        mapperSet.forEach(mapper -> {
            JCTree jcTree = javacTrees.getTree(mapper);
            jcTree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    jcClassDecl.getMembers().forEach(member -> {
                        member.accept(new TreeTranslator() {
                            @Override
                            public void visitVarDef(JCTree.JCVariableDecl jcVariableDecl) {
                                String varType = jcVariableDecl.vartype.type.toString();
                                if (!"java.lang.String".equals(varType)) {
                                    // 限定变量类型必须是String类型，否则抛异常
                                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Type '" + varType + "'" + " is not support.");
                                }
                                jcVariableDecl.init = treeMaker.Literal(mapper.toString() + "." + jcVariableDecl.name.toString());
                            }
                        });
                    });
                }
            });
        });
        return true;
    }

}
