package glslplugin.extensions;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ElementColorProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.text.CharArrayCharSequence;
import glslplugin.lang.elements.declarations.GLSLTypeSpecifier;
import glslplugin.lang.elements.expressions.GLSLExpression;
import glslplugin.lang.elements.expressions.GLSLFunctionCallExpression;
import glslplugin.lang.elements.expressions.GLSLParameterList;
import glslplugin.lang.elements.types.GLSLType;
import glslplugin.lang.elements.types.GLSLTypes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UseJBColor")
public class GLSLColorProvider implements ElementColorProvider {

    @Nullable
    @Override
    public Color getColorFrom(@NotNull PsiElement psiElement) {
        if (psiElement instanceof GLSLTypeSpecifier && isColorType(psiElement) && isConstructorCall(psiElement)) {
            //TODO: fix LineMarker warning (this needs to be registered on leaf nodes only)
            return getGLSLColorFromExpression(psiElement);
        }
        return null;
    }

    @Override
    public void setColorTo(@NotNull PsiElement psiElement, @NotNull Color color) {
        final Document document = PsiDocumentManager.getInstance(psiElement.getProject()).getDocument(psiElement.getContainingFile());
        GLSLParameterList params = getParameterList(psiElement);
        if (params == null) return;

        GLSLType type = ((GLSLTypeSpecifier) psiElement).getType();

        Runnable command = () -> {
            if (type == GLSLTypes.VEC3) {
                boolean useRgb = asFloatList(params.getParameters()).stream().anyMatch(val -> val > 1);
                replaceVecColor(params.getParameters(), color, useRgb);
                return;
            }

            if (type == GLSLTypes.IVEC3) {
                //TODO
                return;
            }

            if (type == GLSLTypes.DVEC3) {
                //TODO
                return;

            }

            if (type == GLSLTypes.UVEC3) {
                //TODO
                return;
            }
        };

        CommandProcessor.getInstance().executeCommand(psiElement.getProject(), command, "Change Color",null, document);

    }

    @NotNull
    private Boolean isColorType(PsiElement element) {
        return ((GLSLTypeSpecifier) element).getType() == GLSLTypes.VEC3  ||
               ((GLSLTypeSpecifier) element).getType() == GLSLTypes.IVEC3 ||
               ((GLSLTypeSpecifier) element).getType() == GLSLTypes.UVEC3 ||
               ((GLSLTypeSpecifier) element).getType() == GLSLTypes.DVEC3;
    }

    @NotNull
    private Boolean isConstructorCall(@NotNull PsiElement element) {
        return element.getParent() instanceof GLSLFunctionCallExpression && ((GLSLFunctionCallExpression) element.getParent()).isConstructor();
    }

    @Nullable
    @Contract(pure = true)
    private GLSLParameterList getParameterList(@NotNull PsiElement element) {
        PsiElement currentSibling = element;
        while(currentSibling != null) {

            if (currentSibling instanceof GLSLParameterList) {
                return (GLSLParameterList) currentSibling;
            }

            currentSibling = currentSibling.getNextSibling();
        }

        return null;
    }

    @Nullable
    private Color getGLSLColorFromExpression(@NotNull PsiElement element) {
        GLSLParameterList params = getParameterList(element);

        if (params != null) {
            try {
                List<Float> args = asFloatList(params.getParameters());
                Boolean isRGB = args.stream().anyMatch(val -> val > 1.0);
                return getGLSLColor(args, isRGB);

            } catch (Exception ignore) {}
        }
        return null;
    }

    @NotNull
    @Contract("_, _ -> new")
    private Color getGLSLColor(@NotNull List<Float> args, @NotNull Boolean isRgb) {

        if (isRgb) {
            int r = args.get(0).intValue();
            int g = args.get(1).intValue();
            int b = args.get(2).intValue();
            return new Color(r, g, b);
        } else {
            return new Color(args.get(0), args.get(1), args.get(2));
        }
    }

    private void replaceVecColor(@NotNull GLSLExpression[] args, @NotNull Color color, boolean useRgb) {
        int[] rgb = getColorValues(color);

        for (int i = 0; i < 3; i++) {
            String replaceValue = getDecimalFormat(args[i].getText()).format( useRgb ? rgb[i] : normalize(rgb[i]));
            ((LeafPsiElement) args[i].getNode().getFirstChildNode()).replaceWithText(replaceValue);
        }
    }

    private List<Float> asFloatList(GLSLExpression[] expressions) {
        return Arrays.stream(expressions)
            .filter((GLSLExpression expr) -> expr.getConstantValue() instanceof Double)
            .map((GLSLExpression expr) -> ((Double) expr.getConstantValue()).floatValue())
            .collect(Collectors.toList());

    }

    private int[] getColorValues(Color color) {
        int[] rgb = new int[3];
        rgb[0] = color.getRed();
        rgb[1] = color.getGreen();
        rgb[2] = color.getBlue();
        return rgb;

    }

    private DecimalFormat getDecimalFormat(String text) {
        char[] frac = text.split("\\.")[1].toCharArray();
        String decimalFormatString = new String(frac);
        return new DecimalFormat(decimalFormatString.isEmpty() ? "0" : "0." + decimalFormatString);
    }

    private Float normalize(Integer rgbVal) {
        return rgbVal / 255.f;
    }
}

