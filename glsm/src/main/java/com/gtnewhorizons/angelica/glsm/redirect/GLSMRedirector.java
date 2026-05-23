package com.gtnewhorizons.angelica.glsm.redirect;

import com.google.common.collect.ImmutableSet;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GLSMRedirector {

    private static final boolean ASSERT_MAIN_THREAD = Boolean.getBoolean("angelica.assertMainThread");
    private static final boolean LOG_SPAM = Boolean.getBoolean("angelica.redirectorLogspam");
    private static final Logger LOGGER = LogManager.getLogger("GLSMRedirector");

    private static final String DRAWABLE = "org/lwjgl/opengl/Drawable";
    private static final String GL_STATE_MANAGER = "com/gtnewhorizons/angelica/glsm/GLStateManager";
    private static final String GL_PREFIX = "org/lwjgl/opengl/GL";
    private static final String ARB_VERTEX_ARRAY_OBJECT = "org/lwjgl/opengl/ARBVertexArrayObject";
    private static final String APPLE_VERTEX_ARRAY_OBJECT = "org/lwjgl/opengl/APPLEVertexArrayObject";
    private static final String PROJECT = "org/lwjgl/util/glu/Project";
    private static final String GLU = "org/lwjgl/util/glu/GLU";
    private static final String VANILLA_GL_STATE_MANAGER = "net/minecraft/client/renderer/GlStateManager";
    private static final String OPEN_GL_HELPER = "net/minecraft/client/renderer/OpenGlHelper";
    private static final String EXT_BLEND_FUNC = "org/lwjgl/opengl/EXTBlendFuncSeparate";
    private static final String ARB_MULTI_TEXTURE = "org/lwjgl/opengl/ARBMultitexture";
    private static final String ARB_SHADER_OBJECTS = "org/lwjgl/opengl/ARBShaderObjects";
    private static final String ARB_INSTANCED_ARRAYS = "org/lwjgl/opengl/ARBInstancedArrays";
    private static final String UNIVERSAL_VAO = "com/gtnewhorizon/gtnhlib/client/opengl/UniversalVAO";
    private static final String MINECRAFT_CLIENT = "net.minecraft.client";
    private static final String SPLASH_PROGRESS = "cpw.mods.fml.client.SplashProgress";

    private static final Set<String> EXCLUDED_MINECRAFT_MAIN_THREAD_CHECKS = ImmutableSet.of(
        "startGame", "func_71384_a",
        "initializeTextures", "func_77474_a"
    );

    private static final Map<String, Map<String, String>> METHOD_REDIRECTS = new HashMap<>(32);
    private static final Map<String, String> GL_METHOD_REDIRECTS = new HashMap<>(256);
    private static final Map<Integer, String> GL_CAP_REDIRECTS = new HashMap<>();
    private static final Map<String, String> TYPE_REDIRECTS = new HashMap<>();
    private static final ClassConstantPoolParser CST_POOL_PARSER;

    private static final String[] CORE_EXCLUSIONS = {
        "org.lwjgl",
        "com.gtnewhorizon.gtnhlib.asm",
        "com.gtnewhorizons.angelica.glsm.",
        "me.eigenraven.lwjgl3ify"
    };

    static {
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_ALPHA_TEST, "AlphaTest");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_BLEND, "Blend");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_DEPTH_TEST, "DepthTest");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_CULL_FACE, "Cull");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_LIGHTING, "Lighting");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, "Texture");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_FOG, "Fog");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL, "RescaleNormal");
        GL_CAP_REDIRECTS.put(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST, "ScissorTest");

        Map<String, String> gl11 = RedirectMap.newMap()
            .add("glEnable")
            .add("glDisable")
            .add("glAlphaFunc")
            .add("glBegin")
            .add("glBindTexture")
            .add("glBlendFunc")
            .add("glCallList")
            .add("glCallLists")
            .add("glClear")
            .add("glClearColor")
            .add("glClearDepth")
            .add("glClearStencil")
            .add("glClipPlane")
            .add("glColor3b")
            .add("glColor3d")
            .add("glColor3f")
            .add("glColor3ub")
            .add("glColor4b")
            .add("glColor4d")
            .add("glColor4f")
            .add("glColor4ub")
            .add("glColorMask")
            .add("glColorMaterial")
            .add("glDeleteLists")
            .add("glGenLists")
            .add("glGenTextures")
            .add("glIsList")
            .add("glDeleteTextures")
            .add("glDepthFunc")
            .add("glDepthMask")
            .add("glDepthRange")
            .add("glDrawArrays")
            .add("glDrawBuffer")
            .add("glDrawElements")
            .add("glEdgeFlag")
            .add("glEnd")
            .add("glEndList")
            .add("glFog")
            .add("glFogf")
            .add("glFogi")
            .add("glFrustum")
            .add("glGetBoolean")
            .add("glGetFloat")
            .add("glGetInteger")
            .add("glGetLight")
            .add("glGetMaterial")
            .add("glGetTexLevelParameteri")
            .add("glGetTexParameterf")
            .add("glGetTexParameteri")
            .add("glIsEnabled")
            .add("glMaterial")
            .add("glMaterialf")
            .add("glMateriali")
            .add("glLight")
            .add("glLightf")
            .add("glLighti")
            .add("glLightModel")
            .add("glLightModelf")
            .add("glLightModeli")
            .add("glLineStipple")
            .add("glLineWidth")
            .add("glListBase")
            .add("glLoadIdentity")
            .add("glLoadMatrix")
            .add("glLogicOp")
            .add("glMatrixMode")
            .add("glMultMatrix")
            .add("glNewList")
            .add("glNormal3b")
            .add("glNormal3d")
            .add("glFlush")
            .add("glFinish")
            .add("glGetError")
            .add("glGetString")
            .add("glGetTexImage")
            .add("glNormal3f")
            .add("glNormal3i")
            .add("glOrtho")
            .add("glPopAttrib")
            .add("glPopMatrix")
            .add("glPushAttrib")
            .add("glPushMatrix")
            .add("glRasterPos2d")
            .add("glRasterPos2f")
            .add("glRasterPos2i")
            .add("glRasterPos3d")
            .add("glRasterPos3f")
            .add("glRasterPos3i")
            .add("glRasterPos4d")
            .add("glRasterPos4f")
            .add("glRasterPos4i")
            .add("glRotated")
            .add("glRotatef")
            .add("glScaled")
            .add("glScalef")
            .add("glShadeModel")
            .add("glTexCoord1d")
            .add("glTexCoord1f")
            .add("glTexCoord2d")
            .add("glTexCoord2f")
            .add("glTexCoord3d")
            .add("glTexCoord3f")
            .add("glTexCoord4d")
            .add("glTexCoord4f")
            .add("glTexEnvi")
            .add("glTexEnvf")
            .add("glTexEnv")
            .add("glGetTexEnv")
            .add("glGetTexEnvi")
            .add("glGetTexEnvf")
            .add("glTexGeni")
            .add("glTexGenf")
            .add("glTexGend")
            .add("glTexGen")
            .add("glGetTexGen")
            .add("glGetTexGeni")
            .add("glGetTexGenf")
            .add("glGetTexGend")
            .add("glTexImage1D")
            .add("glTexImage2D")
            .add("glTexImage3D")
            .add("glTexSubImage1D")
            .add("glTexSubImage2D")
            .add("glCopyTexImage1D")
            .add("glCopyTexImage2D")
            .add("glCopyTexSubImage1D")
            .add("glCopyTexSubImage2D")
            .add("glPixelStoref")
            .add("glPixelStorei")
            .add("glPixelTransferf")
            .add("glPixelTransferi")
            .add("glTexParameter")
            .add("glTexParameterf")
            .add("glTexParameteri")
            .add("glCullFace")
            .add("glFrontFace")
            .add("glHint")
            .add("glPointSize")
            .add("glPolygonMode")
            .add("glPolygonOffset")
            .add("glPolygonStipple")
            .add("glAccum")
            .add("glReadBuffer")
            .add("glSampleCoverage")
            .add("glScissor")
            .add("glStencilFunc")
            .add("glStencilFuncSeparate")
            .add("glStencilMask")
            .add("glStencilMaskSeparate")
            .add("glStencilOp")
            .add("glStencilOpSeparate")
            .add("glTranslated")
            .add("glTranslatef")
            .add("glVertex2d")
            .add("glVertex2f")
            .add("glVertex2i")
            .add("glVertex3d")
            .add("glVertex3f")
            .add("glVertex3i")
            .add("glVertexPointer")
            .add("glColorPointer")
            .add("glNormalPointer")
            .add("glTexCoordPointer")
            .add("glEnableClientState")
            .add("glDisableClientState")
            .add("glInterleavedArrays")
            .add("glPushClientAttrib")
            .add("glPopClientAttrib")
            .add("glViewport")
            .add("glFeedbackBuffer")
            .add("glRenderMode")
            .add("glPassThrough")
            .add("glSelectBuffer")
            .add("glInitNames")
            .add("glPushName")
            .add("glPopName")
            .add("glLoadName");
        Map<String, String> gl12 = RedirectMap.newMap()
            .add("glTexImage3D")
            .add("glTexSubImage3D")
            .add("glCopyTexSubImage3D");
        Map<String, String> gl13 = RedirectMap.newMap()
            .add("glActiveTexture")
            .add("glSampleCoverage")
            .add("glClientActiveTexture")
            .add("glMultiTexCoord2f")
            .add("glMultiTexCoord2d")
            .add("glMultiTexCoord2s");
        Map<String, String> gl14 = RedirectMap.newMap()
            .add("glBlendFuncSeparate", "tryBlendFuncSeparate")
            .add("glBlendColor")
            .add("glBlendEquation");
        Map<String, String> gl15 = RedirectMap.newMap()
            .add("glGenBuffers")
            .add("glBindBuffer")
            .add("glDeleteBuffers")
            .add("glBufferData")
            .add("glBufferSubData")
            .add("glMapBuffer")
            .add("glUnmapBuffer")
            .add("glGetBufferSubData")
            .add("glGetBufferParameteri")
            .add("glIsBuffer");
        Map<String, String> gl20 = RedirectMap.newMap()
            .add("glBlendEquationSeparate")
            .add("glDrawBuffers")
            .add("glStencilFuncSeparate")
            .add("glStencilMaskSeparate")
            .add("glStencilOpSeparate")
            .add("glUseProgram")
            .add("glShaderSource")
            .add("glLinkProgram")
            .add("glDeleteProgram")
            .add("glCreateShader")
            .add("glCompileShader")
            .add("glCreateProgram")
            .add("glAttachShader")
            .add("glDetachShader")
            .add("glValidateProgram")
            .add("glGetUniformLocation")
            .add("glGetAttribLocation")
            .add("glUniform1f")
            .add("glUniform2f")
            .add("glUniform3f")
            .add("glUniform4f")
            .add("glUniform1i")
            .add("glUniform2i")
            .add("glUniform3i")
            .add("glUniform4i")
            .add("glUniform1")
            .add("glUniform2")
            .add("glUniform3")
            .add("glUniform4")
            .add("glUniformMatrix2")
            .add("glUniformMatrix3")
            .add("glUniformMatrix4")
            .add("glDeleteShader")
            .add("glGetShaderi")
            .add("glGetShaderInfoLog")
            .add("glGetProgrami")
            .add("glGetProgramInfoLog")
            .add("glBindAttribLocation")
            .add("glVertexAttrib2f")
            .add("glVertexAttrib2s")
            .add("glVertexAttrib3f")
            .add("glVertexAttrib4f")
            .add("glGetActiveUniform")
            .add("glGetAttachedShaders")
            .add("glGetShaderSource")
            .add("glGetUniform")
            .add("glVertexAttribPointer")
            .add("glEnableVertexAttribArray")
            .add("glDisableVertexAttribArray");
        Map<String, String> gl30 = RedirectMap.newMap()
            .add("glGenVertexArrays")
            .add("glBindVertexArray")
            .add("glDeleteVertexArrays")
            .add("glBindFramebuffer")
            .add("glDeleteFramebuffers")
            .add("glGenFramebuffers")
            .add("glCheckFramebufferStatus")
            .add("glFramebufferTexture2D")
            .add("glVertexAttribIPointer")
            .add("glGenerateMipmap")
            .add("glGetFramebufferAttachmentParameteri")
            .add("glBlitFramebuffer");
        Map<String, String> gl31 = RedirectMap.newMap()
            .add("glDrawElementsInstanced");
        Map<String, String> gl32 = RedirectMap.newMap()
            .add("glFramebufferTexture");
        Map<String, String> gl33 = RedirectMap.newMap()
            .add("glGenSamplers")
            .add("glDeleteSamplers")
            .add("glBindSampler")
            .add("glSamplerParameteri")
            .add("glSamplerParameterf")
            .add("glVertexAttribDivisor");
        Map<String, String> gl42 = RedirectMap.newMap()
            .add("glBindImageTexture")
            .add("glMemoryBarrier")
            .add("glTexStorage2D");
        Map<String, String> gl43 = RedirectMap.newMap()
            .add("glDispatchCompute")
            .add("glClearBufferSubData")
            .add("glBindVertexBuffer")
            .add("glVertexAttribFormat")
            .add("glVertexAttribIFormat")
            .add("glVertexAttribBinding");
        Map<String, String> gl44 = RedirectMap.newMap()
            .add("glBufferStorage")
            .add("glClearTexImage");

        GL_METHOD_REDIRECTS.putAll(gl11);
        GL_METHOD_REDIRECTS.putAll(gl12);
        GL_METHOD_REDIRECTS.putAll(gl13);
        GL_METHOD_REDIRECTS.putAll(gl14);
        GL_METHOD_REDIRECTS.putAll(gl15);
        GL_METHOD_REDIRECTS.putAll(gl20);
        GL_METHOD_REDIRECTS.putAll(gl30);
        GL_METHOD_REDIRECTS.putAll(gl31);
        GL_METHOD_REDIRECTS.putAll(gl32);
        GL_METHOD_REDIRECTS.putAll(gl33);
        GL_METHOD_REDIRECTS.putAll(gl42);
        GL_METHOD_REDIRECTS.putAll(gl43);
        GL_METHOD_REDIRECTS.putAll(gl44);

        METHOD_REDIRECTS.put(OPEN_GL_HELPER, RedirectMap.newMap()
            .add("glBlendFunc", "tryBlendFuncSeparate")
            .add("func_148821_a", "tryBlendFuncSeparate")
            .add("func_153171_g", "glBindFramebuffer")
            .add("func_153174_h", "glDeleteFramebuffers")
            .add("func_153165_e", "glGenFramebuffers")
            .add("func_153167_i", "glCheckFramebufferStatus")
            .add("func_153188_a", "glFramebufferTexture2D")
            .add("setActiveTexture")
            .add("setLightmapTextureCoords")
            .add("isFramebufferEnabled")
        );
        METHOD_REDIRECTS.put(VANILLA_GL_STATE_MANAGER, RedirectMap.newMap()
            .add("pushAttrib", "glPushAttrib")
            .add("popAttrib", "glPopAttrib")
            .add("disableAlpha", "disableAlphaTest")
            .add("enableAlpha", "enableAlphaTest")
            .add("alphaFunc", "glAlphaFunc")
            .add("enableLighting")
            .add("disableLighting")
            .add("enableLight")
            .add("disableLight")
            .add("enableColorMaterial")
            .add("disableColorMaterial")
            .add("colorMaterial", "glColorMaterial")
            .add("glLight")
            .add("glLightModel")
            .add("glNormal3f")
            .add("disableDepth", "disableDepthTest")
            .add("enableDepth", "enableDepthTest")
            .add("depthFunc", "glDepthFunc")
            .add("depthMask", "glDepthMask")
            .add("disableBlend")
            .add("enableBlend")
            .add("blendFunc", "glBlendFunc")
            .add("tryBlendFuncSeparate")
            .add("glBlendEquation")
            .add("enableFog")
            .add("disableFog")
            .add("setFog")
            .add("setFogDensity")
            .add("setFogStart")
            .add("setFogEnd")
            .add("glFog")
            .add("glFogi")
            .add("enableCull")
            .add("disableCull")
            .add("cullFace", "glCullFace")
            .add("glPolygonMode")
            .add("enablePolygonOffset")
            .add("disablePolygonOffset")
            .add("doPolygonOffset", "glPolygonOffset")
            .add("enableColorLogic")
            .add("disableColorLogic")
            .add("colorLogicOp", "glLogicOp")
            .add("enableTexGenCoord")
            .add("disableTexGenCoord")
            .add("texGen")
            .add("setActiveTexture")
            .add("enableTexture2D", "enableTexture")
            .add("disableTexture2D", "disableTexture")
            .add("glTexEnv")
            .add("glTexEnvi")
            .add("glTexEnvf")
            .add("glTexParameterf")
            .add("glTexParameteri")
            .add("glGetTexLevelParameteri")
            .add("generateTexture", "glGenTextures")
            .add("deleteTexture", "glDeleteTextures")
            .add("bindTexture")
            .add("glTexImage2D")
            .add("glTexSubImage2D")
            .add("glCopyTexSubImage2D")
            .add("glGetTexImage")
            .add("enableNormalize")
            .add("disableNormalize")
            .add("shadeModel", "glShadeModel")
            .add("enableRescaleNormal")
            .add("disableRescaleNormal")
            .add("viewport", "glViewport")
            .add("colorMask", "glColorMask")
            .add("clearDepth", "glClearDepth")
            .add("clearColor", "glClearColor")
            .add("clear", "glClear")
            .add("matrixMode", "glMatrixMode")
            .add("loadIdentity", "glLoadIdentity")
            .add("pushMatrix", "glPushMatrix")
            .add("popMatrix", "glPopMatrix")
            .add("getFloat", "glGetFloat")
            .add("ortho", "glOrtho")
            .add("rotate", "glRotatef")
            .add("scale", "glScalef")
            .add("translate", "glTranslatef")
            .add("multMatrix", "glMultMatrix")
            .add("color", "glColor4f")
            .add("glTexCoord2f")
            .add("glVertex3f")
            .add("resetColor", "clearCurrentColor")
            .add("glNormalPointer")
            .add("glTexCoordPointer")
            .add("glVertexPointer")
            .add("glColorPointer")
            .add("glDisableClientState")
            .add("glEnableClientState")
            .add("glBegin")
            .add("glEnd")
            .add("glDrawArrays")
            .add("glLineWidth")
            .add("callList", "glCallList")
            .add("glDeleteLists")
            .add("glNewList")
            .add("glEndList")
            .add("glGenLists")
            .add("glPixelStorei")
            .add("glReadPixels")
            .add("glGetError")
            .add("glGetString")
            .add("glGetInteger")
            .add("enableBlendProfile")
            .add("disableBlendProfile")
        );
        METHOD_REDIRECTS.put(EXT_BLEND_FUNC, RedirectMap.newMap().add("glBlendFuncSeparateEXT", "tryBlendFuncSeparate"));
        METHOD_REDIRECTS.put(ARB_MULTI_TEXTURE, RedirectMap.newMap().add("glActiveTextureARB"));
        METHOD_REDIRECTS.put(ARB_SHADER_OBJECTS, RedirectMap.newMap()
            .add("glUseProgramObjectARB", "glUseProgram")
            .add("glShaderSourceARB", "glShaderSource")
            .add("glLinkProgramARB", "glLinkProgram")
            .add("glCreateShaderObjectARB", "glCreateShader")
            .add("glCompileShaderARB", "glCompileShader")
            .add("glCreateProgramObjectARB", "glCreateProgram")
            .add("glAttachObjectARB", "glAttachShader")
            .add("glDetachObjectARB", "glDetachShader")
            .add("glValidateProgramARB", "glValidateProgram")
            .add("glGetUniformLocationARB", "glGetUniformLocation")
            .add("glGetAttribLocationARB", "glGetAttribLocation")
            .add("glDeleteObjectARB")
            .add("glGetObjectParameterARB")
            .add("glGetObjectParameteriARB")
            .add("glGetInfoLogARB")
            .add("glGetHandleARB")
            .add("glUniform1fARB", "glUniform1f")
            .add("glUniform2fARB", "glUniform2f")
            .add("glUniform3fARB", "glUniform3f")
            .add("glUniform4fARB", "glUniform4f")
            .add("glUniform1iARB", "glUniform1i")
            .add("glUniform2iARB", "glUniform2i")
            .add("glUniform3iARB", "glUniform3i")
            .add("glUniform4iARB", "glUniform4i")
            .add("glUniform1ARB")
            .add("glUniform2ARB")
            .add("glUniform3ARB")
            .add("glUniform4ARB")
            .add("glUniformMatrix2ARB", "glUniformMatrix2")
            .add("glUniformMatrix3ARB", "glUniformMatrix3")
            .add("glUniformMatrix4ARB", "glUniformMatrix4")
            .add("glGetActiveUniformARB", "glGetActiveUniform")
            .add("glGetAttachedObjectsARB", "glGetAttachedShaders")
            .add("glGetShaderSourceARB", "glGetShaderSource")
            .add("glGetUniformARB", "glGetUniform")
        );
        METHOD_REDIRECTS.put(ARB_VERTEX_ARRAY_OBJECT, RedirectMap.newMap()
            .add("glBindVertexArray")
            .add("glDeleteVertexArrays")
        );
        METHOD_REDIRECTS.put(ARB_INSTANCED_ARRAYS, RedirectMap.newMap().add("glVertexAttribDivisorARB"));
        METHOD_REDIRECTS.put(APPLE_VERTEX_ARRAY_OBJECT, RedirectMap.newMap().add("glBindVertexArrayAPPLE", "glBindVertexArray"));
        METHOD_REDIRECTS.put(UNIVERSAL_VAO, RedirectMap.newMap()
            .add("bindVertexArray", "glBindVertexArray")
            .add("deleteVertexArrays", "glDeleteVertexArrays")
        );
        METHOD_REDIRECTS.put(PROJECT, RedirectMap.newMap()
            .add("gluPerspective")
            .add("gluLookAt")
            .add("gluPickMatrix")
        );
        METHOD_REDIRECTS.put(GLU, RedirectMap.newMap()
            .add("gluPerspective")
            .add("gluLookAt")
            .add("gluOrtho2D")
            .add("gluPickMatrix")
        );

        TYPE_REDIRECTS.put("org/lwjgl/util/glu/Sphere", "com/gtnewhorizons/angelica/glsm/compat/lwjgl/AngelicaSphere");
        TYPE_REDIRECTS.put("org/lwjgl/util/glu/Cylinder", "com/gtnewhorizons/angelica/glsm/compat/lwjgl/AngelicaCylinder");
        TYPE_REDIRECTS.put("org/lwjgl/util/glu/Disk", "com/gtnewhorizons/angelica/glsm/compat/lwjgl/AngelicaDisk");
        TYPE_REDIRECTS.put("org/lwjgl/util/glu/PartialDisk", "com/gtnewhorizons/angelica/glsm/compat/lwjgl/AngelicaPartialDisk");

        List<String> stringsToSearch = new ArrayList<>(32);
        stringsToSearch.add(GL_PREFIX);
        stringsToSearch.addAll(TYPE_REDIRECTS.keySet());
        stringsToSearch.addAll(METHOD_REDIRECTS.keySet());
        CST_POOL_PARSER = new ClassConstantPoolParser(stringsToSearch.toArray(new String[0]));
    }

    public String[] getCoreExclusions() {
        return CORE_EXCLUSIONS.clone();
    }

    public boolean shouldTransform(byte[] basicClass) {
        return CST_POOL_PARSER.find(basicClass, true);
    }

    public boolean transformClassNode(String transformedName, ClassNode cn) {
        boolean changed = false;
        boolean isOpenGlHelper = transformedName.equals("net.minecraft.client.renderer.OpenGlHelper");

        for (MethodNode mn : (List<MethodNode>) cn.methods) {
            if (isOpenGlHelper && (mn.name.equals("glBlendFunc") || mn.name.equals("func_148821_a"))) {
                continue;
            }
            boolean redirectInMethod = false;
            for (AbstractInsnNode node : mn.instructions.toArray()) {
                if (node instanceof TypeInsnNode tNode) {
                    if (tNode.getOpcode() == Opcodes.NEW || tNode.getOpcode() == Opcodes.CHECKCAST) {
                        String redirect = TYPE_REDIRECTS.get(tNode.desc);
                        if (redirect != null) {
                            if (LOG_SPAM) {
                                LOGGER.info("Redirecting {} in {} from {} to {}", tNode.getOpcode() == Opcodes.NEW ? "NEW" : "CHECKCAST", transformedName, tNode.desc, redirect);
                            }
                            tNode.desc = redirect;
                            changed = true;
                        }
                    }
                } else if (node instanceof MethodInsnNode mNode) {
                    if (mNode.desc.equals("(I)V") && mNode.owner.startsWith(GL_PREFIX) && (mNode.name.equals("glEnable") || mNode.name.equals("glDisable"))) {
                        AbstractInsnNode prevNode = node.getPrevious();
                        String name = null;
                        if (prevNode instanceof LdcInsnNode ldcNode) {
                            name = GL_CAP_REDIRECTS.get((Integer) ldcNode.cst);
                        } else if (prevNode instanceof IntInsnNode intNode) {
                            name = GL_CAP_REDIRECTS.get(intNode.operand);
                        }
                        if (name != null) {
                            name = mNode.name.equals("glEnable") ? "enable" + name : "disable" + name;
                        }
                        if (LOG_SPAM) {
                            String shortOwner = mNode.owner.substring(mNode.owner.lastIndexOf('/') + 1);
                            if (name == null) {
                                LOGGER.info("Redirecting call in {} from {}.{}(I)V to GLStateManager.{}(I)V", transformedName, shortOwner, mNode.name, mNode.name);
                            } else {
                                LOGGER.info("Redirecting call in {} from {}.{}(I)V to GLStateManager.{}()V", transformedName, shortOwner, mNode.name, name);
                            }
                        }
                        mNode.owner = GL_STATE_MANAGER;
                        if (name != null) {
                            mNode.name = name;
                            mNode.desc = "()V";
                            mn.instructions.remove(prevNode);
                        }
                        changed = true;
                        redirectInMethod = true;
                    } else if (mNode.name.equals("makeCurrent") && mNode.owner.startsWith(DRAWABLE)) {
                        mNode.setOpcode(Opcodes.INVOKESTATIC);
                        mNode.owner = GL_STATE_MANAGER;
                        mNode.desc = "(L" + DRAWABLE + ";)V";
                        mNode.itf = false;
                        changed = true;
                        if (LOG_SPAM) {
                            LOGGER.info("Redirecting call in {} to GLStateManager.makeCurrent()", transformedName);
                        }
                    } else {
                        Map<String, String> redirects = mNode.owner.startsWith(GL_PREFIX) ? GL_METHOD_REDIRECTS : METHOD_REDIRECTS.get(mNode.owner);
                        if (redirects != null) {
                            String glsmName = redirects.get(mNode.name);
                            if (glsmName != null) {
                                if (LOG_SPAM) {
                                    String shortOwner = mNode.owner.substring(mNode.owner.lastIndexOf('/') + 1);
                                    LOGGER.info("Redirecting call in {} from {}.{}{} to GLStateManager.{}{}", transformedName, shortOwner, mNode.name, mNode.desc, glsmName, mNode.desc);
                                }
                                mNode.owner = GL_STATE_MANAGER;
                                mNode.name = glsmName;
                                changed = true;
                                redirectInMethod = true;
                            }
                        }
                    }
                    if (mNode.getOpcode() == Opcodes.INVOKESPECIAL && mNode.name.equals("<init>")) {
                        String redirect = TYPE_REDIRECTS.get(mNode.owner);
                        if (redirect != null) {
                            if (LOG_SPAM) {
                                LOGGER.info("Redirecting <init> in {} from {} to {}", transformedName, mNode.owner, redirect);
                            }
                            mNode.owner = redirect;
                            changed = true;
                        }
                    }
                } else if (node instanceof InvokeDynamicInsnNode dynNode) {
                    for (int i = 0; i < dynNode.bsmArgs.length; i++) {
                        if (!(dynNode.bsmArgs[i] instanceof Handle handle)) {
                            continue;
                        }
                        Map<String, String> redirects = handle.getOwner().startsWith(GL_PREFIX) ? GL_METHOD_REDIRECTS : METHOD_REDIRECTS.get(handle.getOwner());
                        if (redirects == null) {
                            continue;
                        }
                        String glsmName = redirects.get(handle.getName());
                        if (glsmName == null) {
                            continue;
                        }
                        if (LOG_SPAM) {
                            String shortOwner = handle.getOwner().substring(handle.getOwner().lastIndexOf('/') + 1);
                            LOGGER.info("Redirecting invokedynamic handle in {} from {}.{}{} to GLStateManager.{}{}", transformedName, shortOwner, handle.getName(), handle.getDesc(), glsmName, handle.getDesc());
                        }
                        dynNode.bsmArgs[i] = new Handle(handle.getTag(), GL_STATE_MANAGER, glsmName, handle.getDesc());
                        changed = true;
                        redirectInMethod = true;
                    }
                }
            }
            if (ASSERT_MAIN_THREAD && redirectInMethod && !transformedName.startsWith(SPLASH_PROGRESS)
                && !(transformedName.startsWith(MINECRAFT_CLIENT) && EXCLUDED_MINECRAFT_MAIN_THREAD_CHECKS.contains(mn.name))) {
                mn.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, GL_STATE_MANAGER, "assertMainThread", "()V", false));
            }
        }

        return changed;
    }

    private static final class RedirectMap<K> extends HashMap<K, K> {

        static RedirectMap<String> newMap() {
            return new RedirectMap<>();
        }

        RedirectMap<K> add(K name) {
            put(name, name);
            return this;
        }

        RedirectMap<K> add(K name, K newName) {
            put(name, newName);
            return this;
        }
    }
}
