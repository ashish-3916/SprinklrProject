package com.start.intern;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.StandardServletEnvironment;


public class ClassScanner {

    private static final Logger logger = LoggerFactory.getLogger(com.start.intern.ClassScanner.class);
    private static  HashMap<String, Set> dependencyTree = new HashMap<>(); // adjacency list
    private static  Set<Class<?>> interfaceCollections = new HashSet<>();
    private static Set<String> requiredAnnotation = new HashSet<>();
    static final String ROOT = "/Users/ashish/Desktop/CreateFile/";  // path where clone is to be created

    ClassScanner(){}

    /**
     * This finds all the classes in the given package
     * @param packageName
     */
    public static void findAllAnnotatedClassesInPackage(String packageName) {
        final List<Class<?>> result = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false, new StandardServletEnvironment());
         // Add the annotation to be considered while Path Scanning.
        provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Repository.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        provider.addIncludeFilter(new AnnotationTypeFilter(SpringBootApplication.class));
        // Add the annotations to the set requiredAnnotation
        requiredAnnotation.add("SpringBootApplication");
        requiredAnnotation.add("Component");
        requiredAnnotation.add("Service");
        requiredAnnotation.add("Repository");
        requiredAnnotation.add("Controller");
        requiredAnnotation.add("Autowired");
        for (BeanDefinition beanDefinition : provider.findCandidateComponents(packageName)) {
            try {
                result.add(Class.forName(beanDefinition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                logger.warn("Could not resolve class object for bean definition", e);
            }
        }
        for(Class<?> obj : result ){
            createClassFile(obj);
        }
        createInterfaceFile();
        printDependencyTree();
    }

    /**
     * Creates the ClassFile for the given clazz and populates its content
     * @param clazz
     */
    private static void createClassFile(Class<?> clazz){
        String filePath = createFile(clazz);
        populateClassFile(clazz , filePath);
    }

    /**
     * Creates the files with a directory assurance
     * @param clazz
     * @return the file Path where file is created
     */
    private static String createFile(Class<?> clazz){
        String fileName = clazz.getSimpleName();
        String packageName = clazz.getPackage().getName();
        String packagePath =  packageName.replace('.', '/');
        String DIRECTORY = ROOT + packagePath ;
        File file= fileWithDirectoryAssurance(DIRECTORY , fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String filePath = DIRECTORY + "/" + fileName + ".java";
        return  filePath;
    }

    /**
     * makes sure that the files is created at the given location
     * if location doesn't exists, create the directory within the path
     * @param directory
     * @param filename
     * @return File
     */
    private static File fileWithDirectoryAssurance(String directory, String filename) {
        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();
        return new File(directory + "/" + filename +".java");
    }

    /**
     * Makes sure that the file is written properly
     * @param clazz
     * @param filePath
     */
    private static void populateClassFile(Class<?> clazz , String filePath){
        try {
            FileWriter myWriter = new FileWriter(filePath);
            StringBuilder text = fillClassTemplate(clazz);
            myWriter.write(String.valueOf(text));
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * fills the content inside the file
     * @param clazz
     * @return fileContent
     */
    private static StringBuilder fillClassTemplate(Class<?> clazz) {
        String clazzName = clazz.getSimpleName();
        String packageName = clazz.getPackage().getName();
        Set<String> libraries = new HashSet<>();
        Set<String> allDependencies = new TreeSet<>();

        // ------------------- PACKAGE CONTENT -----------------------

        StringBuilder packageContent = new StringBuilder("package " + packageName + ";\n\n");

        // ------------------- CLASS CONTENT -----------------------

        StringBuilder classContent = new StringBuilder("\n");
        Annotation[] clazzAnnotations = clazz.getAnnotations(); // get annotations of the class
        for(Annotation anno : clazzAnnotations){
            String classAnnoLibName = anno.annotationType().getName();
            String classAnnoName = anno.annotationType().getSimpleName();
            if(requiredAnnotation.contains(classAnnoName)){ // if matched filter , include it
                libraries.add(classAnnoLibName);
                classContent.append("@").append(classAnnoName).append("\n");
            }
        }
        classContent.append("public class ").append(clazzName).append(" {\n\n");

        // --------------------- FIELD CONTENT ----------------

        StringBuilder fieldContent = new StringBuilder("");
        Field[] declaredFields = clazz.getDeclaredFields(); //get all the fields in the class
        for (Field field : declaredFields) {
            String fieldType = field.getGenericType().getTypeName();
            String fieldTypeShort = getFieldType(fieldType);
            String fieldName = field.getName();
            String fieldLibName = field.getType().getName();
            if (fieldTypeShort.equals("Logger")) continue;

            Annotation[] fieldAnnotation = field.getDeclaredAnnotations(); // annotation for a particular field
            boolean fieldContainsRequiredAnnotation = false;
            for (Annotation anno : fieldAnnotation) {
                String fieldAnnoLibName = anno.annotationType().getName();
                String fieldAnnoName = anno.annotationType().getSimpleName();
                if (requiredAnnotation.contains(fieldAnnoName)) {  // if matched filter , include it
                    fieldContainsRequiredAnnotation = true;
                    libraries.add(fieldAnnoLibName);
                    fieldContent.append("\t@").append(fieldAnnoName).append("\n");
                }
            }
            if (fieldContainsRequiredAnnotation) { // if included , add it to our file Content
                libraries.add(fieldLibName);
                fieldContent.append("\t").append(fieldTypeShort).append(" ").append(fieldName).append(";\n");
                allDependencies.add(fieldTypeShort);
                Class<?> clz  = field.getType(); // for interface
                String clzName = clz.getName();
                if(clz.isInterface() && !clzName.contains("java.util")){
                    interfaceCollections.add(clz);
                }
            }
        }

        // --------------------- CONSTRUCTOR CONTENT ----------------

        StringBuilder constructorContent = new StringBuilder("\n");

        Constructor<?> [] constructors = clazz.getDeclaredConstructors(); // get all constructors
        for(Constructor<?>  constructor : constructors){
            Annotation[] constructorAnnotations = constructor.getDeclaredAnnotations(); // annotation of a contructor
            boolean constructorContainsRequiredAnnotation = false;
            for(Annotation anno  : constructorAnnotations){
                String constructorAnnoLibName = anno.annotationType().getName();
                String constructorAnnoName = anno.annotationType().getSimpleName();
                if(requiredAnnotation.contains(constructorAnnoName)){ // if Autowired include it
                    constructorContainsRequiredAnnotation = true;
                    libraries.add(constructorAnnoLibName);
                    constructorContent.append("\t@").append(constructorAnnoName).append("\n");
                }
            }
            if(constructorContainsRequiredAnnotation){ // prepare the content of Annotated Constructor.
                String constructorName = clazzName;
                constructorContent.append("\t").append(constructorName).append("(");
                TreeSet<String> initialisedFields = new TreeSet<>();
                Parameter [] constructorParameters = constructor.getParameters();
                for(Parameter constructorParameter : constructorParameters){
                    String parameterName = constructorParameter.getName();
                    String parameterType = constructorParameter.toString().replace(" " + parameterName,"");
                    String parameterTypeName = getFieldType(parameterType);
                    String parameterTypeLib = constructorParameter.getType().getTypeName();
                    fieldContent.append("\t").append(parameterTypeName).append(" ").append(parameterName).append(";\n");
                    constructorContent.append(parameterTypeName).append(" ").append(parameterName).append(",");
                    allDependencies.add(parameterTypeName);
                    libraries.add(parameterTypeLib);
                    initialisedFields.add(parameterName);
                    try {
                        Class<?> clz  = Class.forName(parameterTypeLib); // for interface
                        String clzName = clz.getName();
                        if(clz.isInterface() && !clzName.contains("java.util")){
                            interfaceCollections.add(clz);
                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(constructorParameters.length!=0)
                    constructorContent.deleteCharAt(constructorContent.length() -1); // delete a last comma from parameters
                constructorContent.append("){\n");
                for(String initialisedField : initialisedFields){
                    constructorContent.append("\t\t").append("this.").append(initialisedField).append(" = ").append(initialisedField).append(";\n");
                }
                constructorContent.append("\t}\n");
            }
        }

        // --------------------- LIBRARY CONTENT ----------------

        StringBuilder libraryContent = new StringBuilder("\n");
        for(String s : libraries){
            if(!s.contains(packageName))
                 libraryContent.append("import ").append(s).append(";\n");
        }
        // ----- ADDING DEPENDENCY TO THE THE ADJACENCY LIST ---------

        dependencyTree.put(clazzName, allDependencies);

        // -------------- FINALLY FILE CONTENT ------------------

        StringBuilder fileContent = new StringBuilder(""); // finally get all the file Content in one place.
        fileContent.append(packageContent).append(libraryContent).append(classContent).append(fieldContent).append(constructorContent).append("}");
        return fileContent;
    }

    /**
     * get the simplified name of the field Type
     * @param fieldType
     * @return string
     */
    private static String getFieldType(String fieldType) {
        int index = 0 ;
        for(int i= 0 ; i<fieldType.length(); i++){
            if(fieldType.charAt(i)=='.') index = i + 1;
            else if (fieldType.charAt(i)=='<') break;
        }
        return fieldType.substring(index);
    }
//==========================================================================================================================================================================

    //-------- HANDLING INTERFACES ----------

    /**
     * Creating interface
     */
    private static void createInterfaceFile(){
        for(Class<?> clazz : interfaceCollections ){
            String filePath = createFile(clazz);
            populateInterfaceFile(clazz , filePath);
        }
    }

    /**
     * populating interface just as we did while populating class
     * @param clazz
     * @param filePath
     */
    private static void populateInterfaceFile(Class<?> clazz , String filePath){
        try {
            FileWriter myWriter = new FileWriter(filePath);
            StringBuilder text = fillInterfaceTemplate(clazz);
            myWriter.write(String.valueOf(text));
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * interface is created as a class here.
     * Since we are interested in finding the load time of the class in the new project.
     * Interfaces doesn't play much role in load time , so to make sure it complies at created a class here instead of interface.
     * Also it is diffiicult to predict the behaviour of interface as they extend other interfaces which might be user defined or in-built
     * @param clazz
     * @return
     */
    private  static StringBuilder fillInterfaceTemplate(Class<?> clazz){
        StringBuilder interfaceContent = new StringBuilder("");
        String clazzName = clazz.getSimpleName();
        String packageName = clazz.getPackage().getName();
        interfaceContent.append("package ").append(packageName).append(";\n\n");
        interfaceContent.append("import org.springframework.stereotype.Component;\n\n");
        interfaceContent.append("@Component\n");
        interfaceContent.append("public class ").append(clazzName).append("{\n}");
        return interfaceContent;
    }

//==========================================================================================================================================================================

    /**
     * Prints the dependency tree
     */
    private static void printDependencyTree(){
        for(String parent : dependencyTree.keySet()){
            System.out.println(parent);
            for(Object child : dependencyTree.get(parent)){
                System.out.println("\t|___"+child);
            }
        }
    }
}
