package com.google.copybara.doc;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.copybara.doc.annotations.DocElement;
import com.google.copybara.doc.annotations.DocField;

import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Reads classes annotated with {@link DocElement} or {@link DocField} and generates Markdown
 * documentation.
 */
public class MarkdownGenerator extends BasicAnnotationProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {

    return ImmutableList.of(new ProcessingStep() {
      @Override
      public Set<? extends Class<? extends Annotation>> annotations() {
        return ImmutableSet.<Class<? extends Annotation>>of(DocElement.class);
      }

      @Override
      public Set<Element> process(
          SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
        try {
          processDoc(elementsByAnnotation);
        } catch (ElementException e) {
          processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage(), e.element);
        } catch (IOException e) {
          // Unexpected but we cannot do too much about this and Kind.ERROR makes the build
          // to fail.
          e.printStackTrace();
          processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
        }
        return ImmutableSet.of();
      }
    });
  }

  private void processDoc(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation)
      throws ElementException, IOException {
    Multimap<Element, String> docByElementType = ArrayListMultimap.create();

    for (Element element : elementsByAnnotation.get(DocElement.class)) {
      TypeElement classElement = (TypeElement) element;
      StringBuilder sb = new StringBuilder();

      DocElement annotation = classElement.getAnnotation(DocElement.class);

      sb.append("## ").append(annotation.yamlName()).append("\n");
      sb.append(annotation.description());
      sb.append("\n\n**Fields:**\n");
      sb.append("Name | Description\n");
      sb.append("---- | -----------\n");

      for (Element subElement : classElement.getEnclosedElements()) {
        if (isSetter(subElement)) {
          ExecutableElement setter = (ExecutableElement) subElement;
          DocField fieldAnnotation = getFieldAnnotationOrFail(classElement, setter);
          sb.append(setterToField(setter));
          sb.append(" | ");
          sb.append(fieldAnnotation.required() ? "*required;* " : "*optional;* ");
          sb.append(fieldAnnotation.defaultValue().equals("none") ? ""
              : " *default:" + fieldAnnotation.defaultValue() + ";*");
          sb.append("<br/>");
          sb.append(fieldAnnotation.description());
          sb.append("\n");
        }
      }
      Element elementKind = getAnnotationTypeParam(classElement, "elementKind").asElement();
      Element flags = getAnnotationTypeParam(classElement, "flags").asElement();
      StringBuilder flagsString = new StringBuilder();
      for (Element member : flags.getEnclosedElements()) {
        Parameter flagAnnotation = member.getAnnotation(Parameter.class);
        if (flagAnnotation != null && member instanceof VariableElement) {
          VariableElement field = (VariableElement) member;
          flagsString.append(Joiner.on(", ").join(flagAnnotation.names()));
          flagsString.append(" | *");
          flagsString.append(simplerJavaTypes(field));
          flagsString.append("* | ");
          flagsString.append(flagAnnotation.description());
          flagsString.append("\n");
        }
      }

      if (flagsString.length() > 0) {
        sb.append("\n\n**Command line flags:**\n");
        sb.append("Name | Type | Description\n");
        sb.append("---- | ----------- | -----------\n");
        sb.append(flagsString);
      }

      docByElementType.put(elementKind, sb.toString());
    }

    for (Element group : docByElementType.keySet()) {
      FileObject resource = processingEnv.getFiler()
          .createResource(StandardLocation.SOURCE_OUTPUT, "", group.getSimpleName() + ".md");

      try (Writer writer = resource.openWriter()) {
        writer.append("# ").append(group.getSimpleName()).append("\n\n");
        for (String groupValues : docByElementType.get(group)) {
          writer.append(groupValues).append("\n");
        }
      }
    }
  }


  private String simplerJavaTypes(VariableElement field) {
    String s = field.asType().toString();
    if (s.startsWith("java.lang.")) {
      return deCapitalize(s.substring("java.lang.".length()));
    }
    return s;
  }

  /**
   * Extracts a YAML configuration field name from a Java setter. For example, for 'setField'
   * method, 'field' is returned.
   */
  private String setterToField(ExecutableElement setter) {
    String substring = setter.getSimpleName().toString().substring(3);
    return deCapitalize(substring);
  }

  private String deCapitalize(String substring) {
    return Character.toLowerCase(substring.charAt(0)) + substring.substring(1);
  }

  /**
   * Returns true if an {@code Element} represents a setter method
   */
  private boolean isSetter(Element e) {
    if (!(e instanceof ExecutableElement)) {
      return false;
    }
    ExecutableElement method = (ExecutableElement) e;
    // Probably there is some corner case, but this is good enough...
    String methodName = e.getSimpleName().toString();
    return methodName.startsWith("set")
        && methodName.length() > 3
        && e.getModifiers().contains(Modifier.PUBLIC)
        && (!e.getModifiers().contains(Modifier.STATIC))
        && method.getParameters().size() == 1
        && (method.getReturnType().getKind() == TypeKind.VOID);
  }

  private DocField getFieldAnnotationOrFail(TypeElement cls, ExecutableElement setter)
      throws ElementException {
    DocField annotation = setter.getAnnotation(DocField.class);
    if (annotation == null) {
      throw new ElementException(setter, "'" + cls.getSimpleName() + "." + setter.getSimpleName()
          + "' is not documented using '@" + DocField.class.getSimpleName() + "'");
    }
    return annotation;
  }

  private DeclaredType getAnnotationTypeParam(Element element, String name)
      throws ElementException {
    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
        getAnnotationMirror(element, DocElement.class)
            .getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().toString().equals(name)) {
        return (DeclaredType) entry.getValue().getValue();
      }
    }
    throw new ElementException(element,
        "Cannot find type field" + name + " in class " + element.getSimpleName());
  }

  /**
   * Looks for the {@link AnnotationMirror} in the {@code element} that represents the {@code
   * annotation}.
   */
  private static AnnotationMirror getAnnotationMirror(
      Element element, Class<? extends Annotation> annotation) throws ElementException {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (getQualifiedName(mirror.getAnnotationType())
          .contentEquals(annotation.getCanonicalName())) {
        return mirror;
      }
    }
    throw new ElementException(element,
        element.getSimpleName() + " is not annotated with @" + annotation.getSimpleName());
  }

  private static Name getQualifiedName(DeclaredType type) {
    return ((TypeElement) type.asElement()).getQualifiedName();
  }

  /**
   * Exception that we use internally to abort execution but keep a reference to the failing element
   * in order to report the error correctly.
   */
  private static final class ElementException extends Exception {

    private final Element element;

    private ElementException(Element element, String message) {
      super(message);
      this.element = element;
    }
  }
}