package io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FoodGeoModel {
    private final int textureWidth;
    private final int textureHeight;
    private final List<Part> roots;
    private final Map<String, Part> partsByName;

    private FoodGeoModel(int textureWidth, int textureHeight, List<Part> roots, Map<String, Part> partsByName) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.roots = roots;
        this.partsByName = partsByName;
    }

    public static FoodGeoModel empty() {
        return new FoodGeoModel(16, 16, List.of(), Map.of());
    }

    public static FoodGeoModel fromJson(JsonObject rootJson) {
        if (rootJson == null || !rootJson.has("minecraft:geometry") || !rootJson.get("minecraft:geometry").isJsonArray()) {
            return empty();
        }
        JsonArray geometryArray = rootJson.getAsJsonArray("minecraft:geometry");
        if (geometryArray.size() == 0 || !geometryArray.get(0).isJsonObject()) {
            return empty();
        }

        JsonObject geometry = geometryArray.get(0).getAsJsonObject();
        JsonObject description = geometry.has("description") && geometry.get("description").isJsonObject()
                ? geometry.getAsJsonObject("description")
                : new JsonObject();
        int textureWidth = getInt(description, "texture_width", 16);
        int textureHeight = getInt(description, "texture_height", 16);

        JsonArray bonesArray = geometry.has("bones") && geometry.get("bones").isJsonArray()
                ? geometry.getAsJsonArray("bones")
                : new JsonArray();
        if (bonesArray.size() == 0) {
            return new FoodGeoModel(textureWidth, textureHeight, List.of(), Map.of());
        }

        Map<String, BoneDef> boneDefs = new HashMap<>();
        for (JsonElement element : bonesArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            BoneDef bone = parseBone(element.getAsJsonObject());
            if (bone == null || bone.name == null || bone.name.isEmpty()) {
                continue;
            }
            boneDefs.put(bone.name, bone);
        }

        Map<String, Part> parts = new HashMap<>();
        for (String name : boneDefs.keySet()) {
            parts.put(name, new Part(name));
        }

        List<Part> roots = new ArrayList<>();
        for (BoneDef bone : boneDefs.values()) {
            Part part = parts.get(bone.name);
            if (part == null) {
                continue;
            }

            part.setPos(
                    convertPivot(bone, boneDefs, 0),
                    convertPivot(bone, boneDefs, 1),
                    convertPivot(bone, boneDefs, 2)
            );
            part.setRotation(
                    toRadians(bone.rotationDeg.x()),
                    toRadians(bone.rotationDeg.y()),
                    toRadians(bone.rotationDeg.z())
            );

            if (bone.parent != null && parts.containsKey(bone.parent)) {
                Part parent = parts.get(bone.parent);
                parent.addChild(part);
                part.parent = parent;
            } else {
                roots.add(part);
                part.parent = null;
            }

            for (CubeDef cube : bone.cubes) {
                if (cube.rotationDeg != null && cube.pivot != null) {
                    Part cubePart = new Part(null);
                    cubePart.setPos(
                            convertPivot(bone, cube, 0),
                            convertPivot(bone, cube, 1),
                            convertPivot(bone, cube, 2)
                    );
                    cubePart.setRotation(
                            toRadians(cube.rotationDeg.x()),
                            toRadians(cube.rotationDeg.y()),
                            toRadians(cube.rotationDeg.z())
                    );
                    cubePart.parent = part;
                    cubePart.addCube(createCube(
                            cube,
                            convertOrigin(cube, 0),
                            convertOrigin(cube, 1),
                            convertOrigin(cube, 2),
                            textureWidth,
                            textureHeight
                    ));
                    part.addChild(cubePart);
                } else {
                    part.addCube(createCube(
                            cube,
                            convertOrigin(bone, cube, 0),
                            convertOrigin(bone, cube, 1),
                            convertOrigin(bone, cube, 2),
                            textureWidth,
                            textureHeight
                    ));
                }
            }
        }

        return new FoodGeoModel(textureWidth, textureHeight, roots, parts);
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        render(poseStack, consumer, light, overlay, null);
    }

    public void render(
            PoseStack poseStack,
            VertexConsumer consumer,
            int light,
            int overlay,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        for (Part root : roots) {
            root.render(poseStack, consumer, light, overlay, animatedPose);
        }
    }

    public boolean renderPart(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, String partName) {
        return renderPart(poseStack, consumer, light, overlay, partName, null);
    }

    public boolean renderPart(
            PoseStack poseStack,
            VertexConsumer consumer,
            int light,
            int overlay,
            String partName,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        if (partName == null || partName.isBlank()) {
            return false;
        }
        Part part = partsByName.get(partName);
        if (part == null) {
            return false;
        }
        List<Part> chain = new ArrayList<>();
        Part cursor = part.getParent();
        while (cursor != null) {
            chain.add(0, cursor);
            cursor = cursor.getParent();
        }

        poseStack.pushPose();
        for (Part parent : chain) {
            applyPartTransform(poseStack, parent, animatedPose);
        }
        part.render(poseStack, consumer, light, overlay, animatedPose);
        poseStack.popPose();
        return true;
    }

    public @Nullable List<Part> getPath(String partName) {
        Part part = partsByName.get(partName);
        if (part == null) {
            return null;
        }
        LinkedList<Part> path = new LinkedList<>();
        while (part != null) {
            path.addFirst(part);
            part = part.parent;
        }
        return path;
    }

    private static Cube createCube(CubeDef cube, float x, float y, float z, int textureWidth, int textureHeight) {
        if (!cube.faceUvs.isEmpty()) {
            return new CubePerFace(
                    x, y, z,
                    cube.size.x(), cube.size.y(), cube.size.z(),
                    cube.inflate,
                    textureWidth, textureHeight,
                    cube.faceUvs
            );
        }
        return new CubeBox(
                cube.uvOriginX, cube.uvOriginY,
                x, y, z,
                cube.size.x(), cube.size.y(), cube.size.z(),
                cube.inflate, cube.mirror,
                textureWidth, textureHeight
        );
    }

    private static float convertPivot(BoneDef bone, Map<String, BoneDef> index, int axis) {
        if (bone.parent != null && index.containsKey(bone.parent)) {
            BoneDef parent = index.get(bone.parent);
            if (axis == 1) {
                return parent.pivot.y() - bone.pivot.y();
            }
            return axis == 0 ? bone.pivot.x() - parent.pivot.x() : bone.pivot.z() - parent.pivot.z();
        }
        if (axis == 1) {
            return 24.0f - bone.pivot.y();
        }
        return axis == 0 ? bone.pivot.x() : bone.pivot.z();
    }

    private static float convertPivot(BoneDef parent, CubeDef cube, int axis) {
        if (cube.pivot == null) {
            return 0.0f;
        }
        if (axis == 1) {
            return parent.pivot.y() - cube.pivot.y();
        }
        return axis == 0 ? cube.pivot.x() - parent.pivot.x() : cube.pivot.z() - parent.pivot.z();
    }

    private static float convertOrigin(BoneDef bone, CubeDef cube, int axis) {
        if (axis == 1) {
            return bone.pivot.y() - cube.origin.y() - cube.size.y();
        }
        return axis == 0 ? cube.origin.x() - bone.pivot.x() : cube.origin.z() - bone.pivot.z();
    }

    private static float convertOrigin(CubeDef cube, int axis) {
        if (cube.pivot == null) {
            return 0.0f;
        }
        if (axis == 1) {
            return cube.pivot.y() - cube.origin.y() - cube.size.y();
        }
        return axis == 0 ? cube.origin.x() - cube.pivot.x() : cube.origin.z() - cube.pivot.z();
    }

    private static float toRadians(float degree) {
        return (float) (degree * Math.PI / 180.0);
    }

    private static @Nullable BoneDef parseBone(JsonObject boneObj) {
        if (!boneObj.has("name") || !boneObj.get("name").isJsonPrimitive()) {
            return null;
        }
        BoneDef bone = new BoneDef();
        bone.name = boneObj.get("name").getAsString();
        bone.parent = boneObj.has("parent") && boneObj.get("parent").isJsonPrimitive()
                ? boneObj.get("parent").getAsString() : null;
        bone.pivot = readVector3(boneObj.get("pivot"), new Vector3f());
        bone.rotationDeg = readVector3(boneObj.get("rotation"), new Vector3f());
        bone.cubes = new ArrayList<>();

        if (boneObj.has("cubes") && boneObj.get("cubes").isJsonArray()) {
            for (JsonElement cubeElement : boneObj.getAsJsonArray("cubes")) {
                if (!cubeElement.isJsonObject()) {
                    continue;
                }
                CubeDef cube = parseCube(cubeElement.getAsJsonObject());
                if (cube != null) {
                    bone.cubes.add(cube);
                }
            }
        }
        return bone;
    }

    private static @Nullable CubeDef parseCube(JsonObject cubeObj) {
        if (!cubeObj.has("origin") || !cubeObj.has("size")) {
            return null;
        }
        CubeDef cube = new CubeDef();
        cube.origin = readVector3(cubeObj.get("origin"), new Vector3f());
        cube.size = readVector3(cubeObj.get("size"), new Vector3f());
        cube.inflate = getFloat(cubeObj, "inflate", 0.0f);
        cube.mirror = getBoolean(cubeObj, "mirror", false);
        cube.uvOriginX = 0.0f;
        cube.uvOriginY = 0.0f;
        cube.faceUvs = new EnumMap<>(Direction.class);

        if (cubeObj.has("pivot")) {
            cube.pivot = readVector3(cubeObj.get("pivot"), new Vector3f());
        }
        if (cubeObj.has("rotation")) {
            cube.rotationDeg = readVector3(cubeObj.get("rotation"), new Vector3f());
        }

        if (cubeObj.has("uv")) {
            JsonElement uvElement = cubeObj.get("uv");
            if (uvElement.isJsonArray()) {
                Vector3f uv = readVector3(uvElement, new Vector3f());
                cube.uvOriginX = uv.x();
                cube.uvOriginY = uv.y();
            } else if (uvElement.isJsonObject()) {
                JsonObject uvObj = uvElement.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : uvObj.entrySet()) {
                    Direction direction = parseDirection(entry.getKey());
                    if (direction == null || !entry.getValue().isJsonObject()) {
                        continue;
                    }
                    JsonObject faceObj = entry.getValue().getAsJsonObject();
                    float[] uv = readFloatArray2(faceObj.get("uv"));
                    float[] uvSize = readFloatArray2(faceObj.get("uv_size"));
                    int uvRotation = getInt(faceObj, "uv_rotation", 0);
                    cube.faceUvs.put(direction, new FaceUv(uv[0], uv[1], uvSize[0], uvSize[1], uvRotation));
                }
            }
        }
        return cube;
    }

    private static @Nullable Direction parseDirection(String name) {
        return switch (name) {
            // Keep TACZ/Bedrock-face compatibility:
            // Bedrock per-face UV names are mapped to Minecraft cube polygon sides
            // with up/down and east/west swapped.
            case "down" -> Direction.UP;
            case "up" -> Direction.DOWN;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.EAST;
            case "east" -> Direction.WEST;
            default -> null;
        };
    }

    private static int getInt(JsonObject obj, String key, int def) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return def;
        }
        return obj.get(key).getAsInt();
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return def;
        }
        return obj.get(key).getAsFloat();
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            return def;
        }
        return obj.get(key).getAsBoolean();
    }

    private static Vector3f readVector3(JsonElement element, Vector3f def) {
        if (element == null || !element.isJsonArray()) {
            return new Vector3f(def);
        }
        JsonArray arr = element.getAsJsonArray();
        float x = arr.size() > 0 && arr.get(0).isJsonPrimitive() ? arr.get(0).getAsFloat() : def.x();
        float y = arr.size() > 1 && arr.get(1).isJsonPrimitive() ? arr.get(1).getAsFloat() : def.y();
        float z = arr.size() > 2 && arr.get(2).isJsonPrimitive() ? arr.get(2).getAsFloat() : def.z();
        return new Vector3f(x, y, z);
    }

    private static float[] readFloatArray2(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return new float[]{0.0f, 0.0f};
        }
        JsonArray arr = element.getAsJsonArray();
        float a = arr.size() > 0 && arr.get(0).isJsonPrimitive() ? arr.get(0).getAsFloat() : 0.0f;
        float b = arr.size() > 1 && arr.get(1).isJsonPrimitive() ? arr.get(1).getAsFloat() : 0.0f;
        return new float[]{a, b};
    }

    public static final class Part {
        private final @Nullable String name;
        private final List<Cube> cubes = new ArrayList<>();
        private final List<Part> children = new ArrayList<>();
        private float x;
        private float y;
        private float z;
        private float xRot;
        private float yRot;
        private float zRot;
        private @Nullable Part parent;

        private Part(@Nullable String name) {
            this.name = name;
        }

        private void setPos(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void setRotation(float xRot, float yRot, float zRot) {
            this.xRot = xRot;
            this.yRot = yRot;
            this.zRot = zRot;
        }

        private void addCube(Cube cube) {
            cubes.add(cube);
        }

        private void addChild(Part part) {
            children.add(part);
        }

        public @Nullable String getName() {
            return name;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }

        public float getXRot() {
            return xRot;
        }

        public float getYRot() {
            return yRot;
        }

        public float getZRot() {
            return zRot;
        }

        public @Nullable Part getParent() {
            return parent;
        }

        private void render(
                PoseStack poseStack,
                VertexConsumer consumer,
                int light,
                int overlay,
                @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
        ) {
            poseStack.pushPose();
            PartTransform transform = resolveTransform(this, animatedPose);
            poseStack.translate(transform.x() / 16.0f, transform.y() / 16.0f, transform.z() / 16.0f);
            if (transform.zRot() != 0.0f) {
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(transform.zRot()));
            }
            if (transform.yRot() != 0.0f) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(transform.yRot()));
            }
            if (transform.xRot() != 0.0f) {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(transform.xRot()));
            }
            if (transform.scaleX() != 1.0f || transform.scaleY() != 1.0f || transform.scaleZ() != 1.0f) {
                poseStack.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
            }

            PoseStack.Pose pose = poseStack.last();
            float partDepthBias = computePartDepthBias(name);
            for (Cube cube : cubes) {
                cube.compile(pose, consumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f, partDepthBias);
            }
            for (Part child : children) {
                child.render(poseStack, consumer, light, overlay, animatedPose);
            }
            poseStack.popPose();
        }
    }

    private interface Cube {
        void compile(
                PoseStack.Pose pose,
                VertexConsumer consumer,
                int light,
                int overlay,
                float red,
                float green,
                float blue,
                float alpha,
                float depthBias
        );
    }

    private static final class CubeBox implements Cube {
        private final Polygon[] polygons;

        private CubeBox(float texOffX, float texOffY,
                        float x, float y, float z,
                        float width, float height, float depth,
                        float delta, boolean mirror,
                        float texWidth, float texHeight) {
            this.polygons = new Polygon[6];

            float xEnd = x + width;
            float yEnd = y + height;
            float zEnd = z + depth;
            x -= delta;
            y -= delta;
            z -= delta;
            xEnd += delta;
            yEnd += delta;
            zEnd += delta;

            if (mirror) {
                float t = xEnd;
                xEnd = x;
                x = t;
            }

            Vertex v1 = new Vertex(x, y, z, 0.0f, 0.0f);
            Vertex v2 = new Vertex(xEnd, y, z, 0.0f, 8.0f);
            Vertex v3 = new Vertex(xEnd, yEnd, z, 8.0f, 8.0f);
            Vertex v4 = new Vertex(x, yEnd, z, 8.0f, 0.0f);
            Vertex v5 = new Vertex(x, y, zEnd, 0.0f, 0.0f);
            Vertex v6 = new Vertex(xEnd, y, zEnd, 0.0f, 8.0f);
            Vertex v7 = new Vertex(xEnd, yEnd, zEnd, 8.0f, 8.0f);
            Vertex v8 = new Vertex(x, yEnd, zEnd, 8.0f, 0.0f);

            int dx = (int) width;
            int dy = (int) height;
            int dz = (int) depth;

            float p1 = texOffX + dz;
            float p2 = texOffX + dz + dx;
            float p3 = texOffX + dz + dx + dx;
            float p4 = texOffX + dz + dx + dz;
            float p5 = texOffX + dz + dx + dz + dx;
            float p6 = texOffY + dz;
            float p7 = texOffY + dz + dy;
            float p8 = texOffY;
            float p9 = texOffX;

            this.polygons[2] = new Polygon(new Vertex[]{v6, v5, v1, v2}, p1, p8, p2, p6, texWidth, texHeight, mirror, Direction.DOWN);
            this.polygons[3] = new Polygon(new Vertex[]{v3, v4, v8, v7}, p2, p6, p3, p8, texWidth, texHeight, mirror, Direction.UP);
            this.polygons[1] = new Polygon(new Vertex[]{v1, v5, v8, v4}, p9, p6, p1, p7, texWidth, texHeight, mirror, Direction.WEST);
            this.polygons[4] = new Polygon(new Vertex[]{v2, v1, v4, v3}, p1, p6, p2, p7, texWidth, texHeight, mirror, Direction.NORTH);
            this.polygons[0] = new Polygon(new Vertex[]{v6, v2, v3, v7}, p2, p6, p4, p7, texWidth, texHeight, mirror, Direction.EAST);
            this.polygons[5] = new Polygon(new Vertex[]{v5, v6, v7, v8}, p4, p6, p5, p7, texWidth, texHeight, mirror, Direction.SOUTH);
        }

        @Override
        public void compile(
                PoseStack.Pose pose,
                VertexConsumer consumer,
                int light,
                int overlay,
                float red,
                float green,
                float blue,
                float alpha,
                float depthBias
        ) {
            compilePolygons(polygons, pose, consumer, light, overlay, red, green, blue, alpha, depthBias);
        }
    }

    private static final class CubePerFace implements Cube {
        private final Polygon[] polygons;

        private CubePerFace(float x, float y, float z,
                            float width, float height, float depth,
                            float delta,
                            float texWidth, float texHeight,
                            Map<Direction, FaceUv> faceUvs) {
            this.polygons = new Polygon[6];

            float xEnd = x + width;
            float yEnd = y + height;
            float zEnd = z + depth;
            x -= delta;
            y -= delta;
            z -= delta;
            xEnd += delta;
            yEnd += delta;
            zEnd += delta;

            Vertex v1 = new Vertex(x, y, z, 0.0f, 0.0f);
            Vertex v2 = new Vertex(xEnd, y, z, 0.0f, 8.0f);
            Vertex v3 = new Vertex(xEnd, yEnd, z, 8.0f, 8.0f);
            Vertex v4 = new Vertex(x, yEnd, z, 8.0f, 0.0f);
            Vertex v5 = new Vertex(x, y, zEnd, 0.0f, 0.0f);
            Vertex v6 = new Vertex(xEnd, y, zEnd, 0.0f, 8.0f);
            Vertex v7 = new Vertex(xEnd, yEnd, zEnd, 8.0f, 8.0f);
            Vertex v8 = new Vertex(x, yEnd, zEnd, 8.0f, 0.0f);

            this.polygons[2] = createPolygon(new Vertex[]{v6, v5, v1, v2}, texWidth, texHeight, Direction.DOWN, faceUvs.get(Direction.DOWN));
            this.polygons[3] = createPolygon(new Vertex[]{v3, v4, v8, v7}, texWidth, texHeight, Direction.UP, faceUvs.get(Direction.UP));
            this.polygons[1] = createPolygon(new Vertex[]{v1, v5, v8, v4}, texWidth, texHeight, Direction.WEST, faceUvs.get(Direction.WEST));
            this.polygons[4] = createPolygon(new Vertex[]{v2, v1, v4, v3}, texWidth, texHeight, Direction.NORTH, faceUvs.get(Direction.NORTH));
            this.polygons[0] = createPolygon(new Vertex[]{v6, v2, v3, v7}, texWidth, texHeight, Direction.EAST, faceUvs.get(Direction.EAST));
            this.polygons[5] = createPolygon(new Vertex[]{v5, v6, v7, v8}, texWidth, texHeight, Direction.SOUTH, faceUvs.get(Direction.SOUTH));
        }

        private static @Nullable Polygon createPolygon(Vertex[] vertices, float texWidth, float texHeight, Direction direction, @Nullable FaceUv faceUv) {
            if (faceUv == null) {
                return null;
            }
            return new Polygon(
                    vertices,
                    faceUv.u,
                    faceUv.v,
                    faceUv.u + faceUv.uvSizeX,
                    faceUv.v + faceUv.uvSizeY,
                    texWidth,
                    texHeight,
                    false,
                    direction,
                    faceUv.uvRotationDeg
            );
        }

        @Override
        public void compile(
                PoseStack.Pose pose,
                VertexConsumer consumer,
                int light,
                int overlay,
                float red,
                float green,
                float blue,
                float alpha,
                float depthBias
        ) {
            compilePolygons(polygons, pose, consumer, light, overlay, red, green, blue, alpha, depthBias);
        }
    }

    private static void compilePolygons(Polygon[] polygons, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                                        float red, float green, float blue, float alpha, float depthBias) {
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        for (Polygon polygon : polygons) {
            if (polygon == null) {
                continue;
            }
            if (isDegeneratePolygon(polygon)) {
                continue;
            }
            Vector3f normal = new Vector3f(polygon.normal);
            normal.mul(matrix3f).normalize();
            float nx = normal.x();
            float ny = normal.y();
            float nz = normal.z();
            for (Vertex vertex : polygon.vertices) {
                float x = vertex.pos.x() / 16.0f + nx * depthBias;
                float y = vertex.pos.y() / 16.0f + ny * depthBias;
                float z = vertex.pos.z() / 16.0f + nz * depthBias;
                consumer.addVertex(matrix4f, x, y, z)
                        .setColor(red, green, blue, alpha)
                        .setUv(vertex.u, vertex.v)
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(nx, ny, nz);
            }
        }
    }

    private static boolean isDegeneratePolygon(Polygon polygon) {
        if (polygon.vertices.length < 3) {
            return true;
        }
        Vector3f a = polygon.vertices[0].pos;
        Vector3f b = polygon.vertices[1].pos;
        Vector3f c = polygon.vertices[2].pos;
        float abx = b.x() - a.x();
        float aby = b.y() - a.y();
        float abz = b.z() - a.z();
        float acx = c.x() - a.x();
        float acy = c.y() - a.y();
        float acz = c.z() - a.z();
        float cx = aby * acz - abz * acy;
        float cy = abz * acx - abx * acz;
        float cz = abx * acy - aby * acx;
        float area2 = cx * cx + cy * cy + cz * cz;
        return area2 <= 0.0000001f;
    }

    private static float computePartDepthBias(@Nullable String partName) {
        if (partName == null || partName.isBlank()) {
            return 0.0f;
        }
        int hash = partName.hashCode();
        float step = 0.00002f * (1 + (hash & 0x7));
        return ((hash & 0x8) == 0 ? 1.0f : -1.0f) * step;
    }

    private static void applyPartTransform(
            PoseStack poseStack,
            Part part,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        PartTransform transform = resolveTransform(part, animatedPose);
        poseStack.translate(transform.x() / 16.0f, transform.y() / 16.0f, transform.z() / 16.0f);
        if (transform.zRot() != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(transform.zRot()));
        }
        if (transform.yRot() != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(transform.yRot()));
        }
        if (transform.xRot() != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation(transform.xRot()));
        }
        if (transform.scaleX() != 1.0f || transform.scaleY() != 1.0f || transform.scaleZ() != 1.0f) {
            poseStack.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
        }
    }

    private static PartTransform resolveTransform(
            Part part,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        float px = part.x;
        float py = part.y;
        float pz = part.z;
        float rx = part.xRot;
        float ry = part.yRot;
        float rz = part.zRot;
        float sx = 1.0f;
        float sy = 1.0f;
        float sz = 1.0f;

        if (part.name != null && animatedPose != null) {
            FoodAnimationController.BonePose pose = animatedPose.get(part.name);
            if (pose != null) {
                px += pose.position().x();
                py -= pose.position().y();
                pz += pose.position().z();

                rx += toRadians(pose.rotationDeg().x());
                ry += toRadians(pose.rotationDeg().y());
                rz += toRadians(pose.rotationDeg().z());

                sx *= pose.scale().x();
                sy *= pose.scale().y();
                sz *= pose.scale().z();
            }
        }

        return new PartTransform(px, py, pz, rx, ry, rz, sx, sy, sz);
    }

    private record PartTransform(
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
    }

    private static final class Polygon {
        private final Vertex[] vertices;
        private final Vector3f normal;

        private Polygon(
                Vertex[] vertices,
                float u1,
                float v1,
                float u2,
                float v2,
                float texWidth,
                float texHeight,
                boolean mirror,
                Direction direction
        ) {
            this(vertices, u1, v1, u2, v2, texWidth, texHeight, mirror, direction, 0);
        }

        private Polygon(
                Vertex[] vertices,
                float u1,
                float v1,
                float u2,
                float v2,
                float texWidth,
                float texHeight,
                boolean mirror,
                Direction direction,
                int uvRotationDeg
        ) {
            this.vertices = vertices;
            float[][] uv = new float[][]{
                    {u2 / texWidth, v1 / texHeight},
                    {u1 / texWidth, v1 / texHeight},
                    {u1 / texWidth, v2 / texHeight},
                    {u2 / texWidth, v2 / texHeight}
            };
            float[][] rotatedUv = rotateUv(uv, uvRotationDeg);
            this.vertices[0] = this.vertices[0].remap(rotatedUv[0][0], rotatedUv[0][1]);
            this.vertices[1] = this.vertices[1].remap(rotatedUv[1][0], rotatedUv[1][1]);
            this.vertices[2] = this.vertices[2].remap(rotatedUv[2][0], rotatedUv[2][1]);
            this.vertices[3] = this.vertices[3].remap(rotatedUv[3][0], rotatedUv[3][1]);

            if (mirror) {
                int len = this.vertices.length;
                for (int i = 0; i < len / 2; i++) {
                    Vertex tmp = this.vertices[i];
                    this.vertices[i] = this.vertices[len - 1 - i];
                    this.vertices[len - 1 - i] = tmp;
                }
            }
            this.normal = new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
            if (mirror) {
                this.normal.mul(-1.0f, 1.0f, 1.0f);
            }
        }

        private static float[][] rotateUv(float[][] uv, int uvRotationDeg) {
            int turns = Math.floorMod(Math.round(uvRotationDeg / 90.0f), 4);
            if (turns == 0) {
                return uv;
            }
            float[][] rotated = new float[4][2];
            for (int i = 0; i < 4; i++) {
                int src = Math.floorMod(i + turns, 4);
                rotated[i][0] = uv[src][0];
                rotated[i][1] = uv[src][1];
            }
            return rotated;
        }
    }

    private static final class Vertex {
        private final Vector3f pos;
        private final float u;
        private final float v;

        private Vertex(float x, float y, float z, float u, float v) {
            this(new Vector3f(x, y, z), u, v);
        }

        private Vertex(Vector3f pos, float u, float v) {
            this.pos = pos;
            this.u = u;
            this.v = v;
        }

        private Vertex remap(float u, float v) {
            return new Vertex(this.pos, u, v);
        }
    }

    private static final class FaceUv {
        private final float u;
        private final float v;
        private final float uvSizeX;
        private final float uvSizeY;
        private final int uvRotationDeg;

        private FaceUv(float u, float v, float uvSizeX, float uvSizeY, int uvRotationDeg) {
            this.u = u;
            this.v = v;
            this.uvSizeX = uvSizeX;
            this.uvSizeY = uvSizeY;
            this.uvRotationDeg = uvRotationDeg;
        }
    }

    private static final class BoneDef {
        private String name;
        private @Nullable String parent;
        private Vector3f pivot;
        private Vector3f rotationDeg;
        private List<CubeDef> cubes;
    }

    private static final class CubeDef {
        private Vector3f origin;
        private Vector3f size;
        private @Nullable Vector3f pivot;
        private @Nullable Vector3f rotationDeg;
        private boolean mirror;
        private float inflate;
        private float uvOriginX;
        private float uvOriginY;
        private Map<Direction, FaceUv> faceUvs;
    }
}
