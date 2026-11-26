package com.hivemind.fabric.client;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
public final class HiveRenderer<T extends MobEntity> extends MobEntityRenderer<T, HiveRenderer.Model<T>> {
    private final Identifier texture;
    public HiveRenderer(EntityRendererFactory.Context context, String caste) {
        super(context, new Model<T>(makeBody(caste), caste.equals("queen")), caste.equals("queen") ? 1.2f : 0.65f);
        texture = new Identifier("hivemind", "textures/entity/" + caste + ".png");
    }
    @Override public Identifier getTexture(T entity) { return texture; }
    private static ModelPart makeBody(String caste) {
        ModelData mesh = new ModelData();
        ModelPartData root = mesh.getRoot();
        boolean queen = caste.equals("queen");
        boolean scout = caste.equals("scout");
        boolean warrior = caste.equals("warrior");
        boolean worker = caste.equals("worker");
        float shoulders = warrior ? 8 : worker ? 7 : queen ? 6 : 4;
        float shoulderY = scout ? 16 : 13;
        ModelPartData thorax = root.addChild("thorax",
                ModelPartBuilder.create().uv(0, 24).cuboid(-shoulders, -3, -6, shoulders * 2, 7, 11),
                ModelTransform.of(0, shoulderY, -1, -0.19f, 0, 0));
        ModelPartData abdomen = root.addChild("abdomen",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4, -3, -2, 8, 6, 9),
                ModelTransform.pivot(0, shoulderY + 1, 4));
        int rings = queen ? 6 : scout ? 5 : 3;
        for (int i = 0; i < rings; i++) {
            float girth = queen ? 8 - Math.abs(i - 2) : scout ? 3.8f - i * 0.55f : 5 - i;
            float depth = scout ? 3.5f : 4;
            abdomen.addChild("haunch" + i,
                    ModelPartBuilder.create().uv(0, 43).cuboid(-girth, -girth * 0.6f, -1, 2 * girth, girth + 1, depth)
                            .uv(0, 66).cuboid(-girth - 0.4f, -girth * 0.6f - 1, -1.4f, 2 * girth + 0.8f, 2, depth),
                    ModelTransform.of(0, (scout ? -0.28f : 0.28f) * i, 3 + i * (depth - 0.6f),
                            -0.12f - 0.035f * i, scout ? i * 0.065f : 0, 0));
        }
        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(72, 0).cuboid(-3.5f, -3, -8, 7, 5, 8)
                        .uv(0, 66).cuboid(-2.5f, -3.7f, -11, 5, 3, 9),
                ModelTransform.pivot(0, shoulderY + 1, -6));
        head.addChild("throat",
                ModelPartBuilder.create().uv(0, 43).cuboid(-2.5f, 0, -5, 5, 3, 5),
                ModelTransform.pivot(0, 1, -2));
        for (int j = 0; j < 2; j++) {
            boolean left = j == 0;
            float side = left ? 1 : -1;
            String suffix = left ? "l" : "r";
            float horn = queen ? 9 : warrior ? 7 : worker ? 4 : 3;
            ModelPartData hornRoot = head.addChild("temple_" + suffix,
                    ModelPartBuilder.create().uv(0, 66).cuboid(-1.5f, -1.5f, 0, 3, 3, horn),
                    ModelTransform.of(side * 3, -1.4f, -6, queen ? 0.8f : 0.42f, side * 0.62f, 0));
            hornRoot.addChild("spur",
                    ModelPartBuilder.create().uv(110, 64).cuboid(-0.68f, -0.68f, -0.4f, 1.36f, 1.36f, 3),
                    ModelTransform.of(0, 0, horn, -0.3f, side * 0.18f, 0));
            for (int i = 0; i < 3; i++) {
                float span = (warrior ? 5 : queen ? 4 : worker ? 3 : 2) - i * 0.35f;
                thorax.addChild("hood_" + suffix + i,
                        ModelPartBuilder.create().uv(0, 24).cuboid(-span / 2, -2, -3, span, 3, 6),
                        ModelTransform.of(side * (shoulders - i * 0.5f), -2 + i * 0.45f, -4 + i * 3.2f,
                                -0.31f - i * 0.08f, side * 0.24f, -side * (warrior ? 0.65f : 0.4f)));
            }
            if (queen) {
                thorax.addChild("royal_fan_" + suffix,
                        ModelPartBuilder.create().uv(0, 66).cuboid(-1, -7, -2, 2, 8, 9),
                        ModelTransform.of(side * 4, -2, -1, -0.55f, side * 0.22f, side * 0.42f));
            } else if (scout) {
                head.addChild("ear_cup_" + suffix,
                        ModelPartBuilder.create().uv(0, 43).cuboid(-1, -3, -1, 2, 5, 6)
                                .uv(0, 66).cuboid(-1.35f, -4, 0, 2.7f, 2, 7),
                        ModelTransform.of(side * 3.8f, -1.2f, -1, 0.36f, side * 0.6f, side * 0.35f));
            } else if (warrior) {
                head.addChild("ram_" + suffix,
                        ModelPartBuilder.create().uv(0, 24).cuboid(-1.8f, -2, -8, 3.6f, 4, 10),
                        ModelTransform.of(side * 3, -1.7f, -4, -0.19f, -side * 0.22f, -side * 0.35f));
            }
            ModelPartData jaw = head.addChild("jaw_" + suffix,
                    ModelPartBuilder.create().uv(72, 16).cuboid(-1.25f, 0, -8, 2.5f, 1.8f, 8),
                    ModelTransform.of(side * 2.6f, 2, -2, 0.13f, -side * 0.09f, 0));
            for (int i = 0; i < 3; i++) {
                float fang = warrior && i == 0 ? 4 : 1.4f + i * 0.22f;
                jaw.addChild("incisor" + i,
                        ModelPartBuilder.create().uv(110, 64).cuboid(-0.5f, -fang, -0.55f, 1, fang, 1.1f),
                        ModelTransform.of(0, 0.25f, -7 + i * 2.4f, -0.27f, 0, side * 0.14f));
            }
        }
        for (int i = 0; i < 4; i++) {
            float side = (i & 1) == 0 ? 1 : -1;
            boolean front = i < 2;
            float bulk = front ? (worker ? 5.5f : warrior ? 5 : 3.5f) : 3;
            ModelPartData leg = root.addChild("leg" + i,
                    ModelPartBuilder.create().uv(64, 44).cuboid(-bulk / 2, -1, -bulk / 2, bulk, front ? 7 : 5, bulk),
                    ModelTransform.of(side * (front ? shoulders - 1 : 3.7f), front ? shoulderY : shoulderY + 2,
                            front ? -3.5f : 8, front ? -0.55f : 0.64f, 0, -side * 0.25f));
            ModelPartData lower = leg.addChild("lower",
                    ModelPartBuilder.create().uv(76, 54).cuboid(-1.4f, 0, -1.6f, 2.8f, scout ? 4 : 6, 3.2f),
                    ModelTransform.of(0, front ? 5.2f : 3.8f, 0, front ? 0.85f : -1.08f, 0, side * 0.15f));
            if (worker && front) {
                lower.addChild("spade",
                        ModelPartBuilder.create().uv(0, 66).cuboid(-2.8f, -0.4f, -3, 5.6f, 5, 3.5f),
                        ModelTransform.of(0, 0.5f, -0.6f, -0.16f, 0, side * 0.08f));
            }
            float paw = worker && front ? 3.3f : 2.15f;
            ModelPartData pawPad = lower.addChild("paw",
                    ModelPartBuilder.create().uv(0, 66).cuboid(-paw, -1, -4, 2 * paw, 2, 5),
                    ModelTransform.of(0, scout ? 3 : 5, 0, front ? -0.3f : 0.44f, 0, 0));
            for (int digit = -1; digit <= 1; digit++) {
                pawPad.addChild("talon" + digit,
                        ModelPartBuilder.create().uv(110, 64).cuboid(-0.5f, 0, -3, 1, 1, 3),
                        ModelTransform.of(digit * paw * 0.65f, -0.35f, -3.6f,
                                0.16f, -digit * 0.15f, 0));
            }
        }
        return TexturedModelData.of(mesh, 128, 128).createModel();
    }

    public static final class Model<T extends MobEntity> extends SinglePartEntityModel<T> {
        private final ModelPart root, head;
        private final boolean queen;
        private final ModelPart[] legs = new ModelPart[4];
        Model(ModelPart root, boolean queen) { this.root=root; this.queen=queen; head=root.getChild("head"); for(int i=0;i<legs.length;i++) legs[i]=root.getChild("leg"+i); }
        @Override public ModelPart getPart() { return root; }
        @Override public void setAngles(T entity, float limb, float speed, float age, float yaw, float pitch) {
            float bodyScale = queen ? 2.0f : 1.0f;
            root.xScale = root.zScale = bodyScale;
            root.yScale = queen ? 3.0f : 1.0f;
            root.pivotY = queen ? -48.0f : 0.0f;
            head.yaw = yaw * ((float)Math.PI/180f) * 0.35f;
            head.pitch = pitch * ((float)Math.PI/180f) * 0.25f;
            float bite = MathHelper.sin(handSwingProgress * (float)Math.PI);
            head.getChild("jaw_l").pitch = 0.13f + bite * 0.48f;
            head.getChild("jaw_r").pitch = 0.13f + bite * 0.48f;
            head.getChild("throat").pitch = MathHelper.sin(age * 0.13f) * 0.09f;
            root.getChild("abdomen").pitch = MathHelper.sin(age * 0.065f) * 0.05f;
            for (int i = 0; i < legs.length; i++) {
                boolean front = i < 2;
                float phase = i == 0 || i == 3 ? 0 : (float) Math.PI;
                float stride = limb * 0.78f + phase;
                legs[i].pitch = (front ? -0.55f : 0.64f) + MathHelper.cos(stride) * speed * 0.48f;
                legs[i].getChild("lower").pitch = (front ? 0.85f : -1.08f)
                        + MathHelper.sin(stride) * speed * 0.28f - (front ? bite * 0.3f : 0);
            }
        }
    }
}