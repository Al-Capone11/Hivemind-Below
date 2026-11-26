package com.hivemind.fabric.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

public final class HiveRenderer<T extends Mob> extends MobRenderer<T, HiveRenderer.State, HiveRenderer.Model> {
    private final Identifier texture;

    public HiveRenderer(EntityRendererProvider.Context context, String caste) {
        super(context, new Model(makeBody(caste), caste.equals("queen")), caste.equals("queen") ? 1.2f : 0.65f);
        texture = Identifier.fromNamespaceAndPath("hivemind", "textures/entity/" + caste + ".png");
    }

    public static final class State extends LivingEntityRenderState { public float bite; }

    @Override public State createRenderState() { return new State(); }
    @Override public Identifier getTextureLocation(State state) { return texture; }
    @Override public void extractRenderState(T entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.bite = entity.getAttackAnim(partialTick);
    }

    private static ModelPart makeBody(String caste) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        boolean queen = caste.equals("queen");
        boolean scout = caste.equals("scout");
        boolean warrior = caste.equals("warrior");
        boolean worker = caste.equals("worker");
        float shoulders = warrior ? 8 : worker ? 7 : queen ? 6 : 4;
        float shoulderY = scout ? 16 : 13;
        PartDefinition thorax = root.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 24).addBox(-shoulders, -3, -6, shoulders * 2, 7, 11),
                PartPose.offsetAndRotation(0, shoulderY, -1, -0.19f, 0, 0));
        PartDefinition abdomen = root.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, -2, 8, 6, 9),
                PartPose.offset(0, shoulderY + 1, 4));
        int rings = queen ? 6 : scout ? 5 : 3;
        for (int i = 0; i < rings; i++) {
            float girth = queen ? 8 - Math.abs(i - 2) : scout ? 3.8f - i * 0.55f : 5 - i;
            float depth = scout ? 3.5f : 4;
            abdomen.addOrReplaceChild("haunch" + i,
                    CubeListBuilder.create().texOffs(0, 43).addBox(-girth, -girth * 0.6f, -1, 2 * girth, girth + 1, depth)
                            .texOffs(0, 66).addBox(-girth - 0.4f, -girth * 0.6f - 1, -1.4f, 2 * girth + 0.8f, 2, depth),
                    PartPose.offsetAndRotation(0, (scout ? -0.28f : 0.28f) * i, 3 + i * (depth - 0.6f),
                            -0.12f - 0.035f * i, scout ? i * 0.065f : 0, 0));
        }
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(72, 0).addBox(-3.5f, -3, -8, 7, 5, 8)
                        .texOffs(0, 66).addBox(-2.5f, -3.7f, -11, 5, 3, 9),
                PartPose.offset(0, shoulderY + 1, -6));
        head.addOrReplaceChild("throat",
                CubeListBuilder.create().texOffs(0, 43).addBox(-2.5f, 0, -5, 5, 3, 5),
                PartPose.offset(0, 1, -2));
        for (int j = 0; j < 2; j++) {
            boolean left = j == 0;
            float side = left ? 1 : -1;
            String suffix = left ? "l" : "r";
            float horn = queen ? 9 : warrior ? 7 : worker ? 4 : 3;
            PartDefinition hornRoot = head.addOrReplaceChild("temple_" + suffix,
                    CubeListBuilder.create().texOffs(0, 66).addBox(-1.5f, -1.5f, 0, 3, 3, horn),
                    PartPose.offsetAndRotation(side * 3, -1.4f, -6, queen ? 0.8f : 0.42f, side * 0.62f, 0));
            hornRoot.addOrReplaceChild("spur",
                    CubeListBuilder.create().texOffs(110, 64).addBox(-0.68f, -0.68f, -0.4f, 1.36f, 1.36f, 3),
                    PartPose.offsetAndRotation(0, 0, horn, -0.3f, side * 0.18f, 0));
            for (int i = 0; i < 3; i++) {
                float span = (warrior ? 5 : queen ? 4 : worker ? 3 : 2) - i * 0.35f;
                thorax.addOrReplaceChild("hood_" + suffix + i,
                        CubeListBuilder.create().texOffs(0, 24).addBox(-span / 2, -2, -3, span, 3, 6),
                        PartPose.offsetAndRotation(side * (shoulders - i * 0.5f), -2 + i * 0.45f, -4 + i * 3.2f,
                                -0.31f - i * 0.08f, side * 0.24f, -side * (warrior ? 0.65f : 0.4f)));
            }
            if (queen) {
                thorax.addOrReplaceChild("royal_fan_" + suffix,
                        CubeListBuilder.create().texOffs(0, 66).addBox(-1, -7, -2, 2, 8, 9),
                        PartPose.offsetAndRotation(side * 4, -2, -1, -0.55f, side * 0.22f, side * 0.42f));
            } else if (scout) {
                head.addOrReplaceChild("ear_cup_" + suffix,
                        CubeListBuilder.create().texOffs(0, 43).addBox(-1, -3, -1, 2, 5, 6)
                                .texOffs(0, 66).addBox(-1.35f, -4, 0, 2.7f, 2, 7),
                        PartPose.offsetAndRotation(side * 3.8f, -1.2f, -1, 0.36f, side * 0.6f, side * 0.35f));
            } else if (warrior) {
                head.addOrReplaceChild("ram_" + suffix,
                        CubeListBuilder.create().texOffs(0, 24).addBox(-1.8f, -2, -8, 3.6f, 4, 10),
                        PartPose.offsetAndRotation(side * 3, -1.7f, -4, -0.19f, -side * 0.22f, -side * 0.35f));
            }
            PartDefinition jaw = head.addOrReplaceChild("jaw_" + suffix,
                    CubeListBuilder.create().texOffs(72, 16).addBox(-1.25f, 0, -8, 2.5f, 1.8f, 8),
                    PartPose.offsetAndRotation(side * 2.6f, 2, -2, 0.13f, -side * 0.09f, 0));
            for (int i = 0; i < 3; i++) {
                float fang = warrior && i == 0 ? 4 : 1.4f + i * 0.22f;
                jaw.addOrReplaceChild("incisor" + i,
                        CubeListBuilder.create().texOffs(110, 64).addBox(-0.5f, -fang, -0.55f, 1, fang, 1.1f),
                        PartPose.offsetAndRotation(0, 0.25f, -7 + i * 2.4f, -0.27f, 0, side * 0.14f));
            }
        }
        for (int i = 0; i < 4; i++) {
            float side = (i & 1) == 0 ? 1 : -1;
            boolean front = i < 2;
            float bulk = front ? (worker ? 5.5f : warrior ? 5 : 3.5f) : 3;
            PartDefinition leg = root.addOrReplaceChild("leg" + i,
                    CubeListBuilder.create().texOffs(64, 44).addBox(-bulk / 2, -1, -bulk / 2, bulk, front ? 7 : 5, bulk),
                    PartPose.offsetAndRotation(side * (front ? shoulders - 1 : 3.7f), front ? shoulderY : shoulderY + 2,
                            front ? -3.5f : 8, front ? -0.55f : 0.64f, 0, -side * 0.25f));
            PartDefinition lower = leg.addOrReplaceChild("lower",
                    CubeListBuilder.create().texOffs(76, 54).addBox(-1.4f, 0, -1.6f, 2.8f, scout ? 4 : 6, 3.2f),
                    PartPose.offsetAndRotation(0, front ? 5.2f : 3.8f, 0, front ? 0.85f : -1.08f, 0, side * 0.15f));
            if (worker && front) {
                lower.addOrReplaceChild("spade",
                        CubeListBuilder.create().texOffs(0, 66).addBox(-2.8f, -0.4f, -3, 5.6f, 5, 3.5f),
                        PartPose.offsetAndRotation(0, 0.5f, -0.6f, -0.16f, 0, side * 0.08f));
            }
            float paw = worker && front ? 3.3f : 2.15f;
            PartDefinition pawPad = lower.addOrReplaceChild("paw",
                    CubeListBuilder.create().texOffs(0, 66).addBox(-paw, -1, -4, 2 * paw, 2, 5),
                    PartPose.offsetAndRotation(0, scout ? 3 : 5, 0, front ? -0.3f : 0.44f, 0, 0));
            for (int digit = -1; digit <= 1; digit++) {
                pawPad.addOrReplaceChild("talon" + digit,
                        CubeListBuilder.create().texOffs(110, 64).addBox(-0.5f, 0, -3, 1, 1, 3),
                        PartPose.offsetAndRotation(digit * paw * 0.65f, -0.35f, -3.6f,
                                0.16f, -digit * 0.15f, 0));
            }
        }
        return LayerDefinition.create(mesh, 128, 128).bakeRoot();
    }

    public static final class Model extends EntityModel<State> {
        private final ModelPart[] legs = new ModelPart[4];
        private final ModelPart head;
        private final boolean queen;

        Model(ModelPart root, boolean queen) {
            super(root);
            this.queen = queen;
            head = root.getChild("head");
            for (int i = 0; i < legs.length; i++) legs[i] = root.getChild("leg" + i);
        }

        @Override public void setupAnim(State state) {
            super.setupAnim(state);
            root.xScale = root.zScale = queen ? 2.0f : 1.0f;
            root.yScale = queen ? 3.0f : 1.0f;
            root.y = queen ? -48.0f : 0.0f;
            head.yRot = state.yRot * ((float) Math.PI / 180f) * 0.3f;
            head.xRot = state.xRot * ((float) Math.PI / 180f) * 0.22f;
            float bite = Mth.sin(state.bite * (float) Math.PI);
            head.getChild("jaw_l").xRot = 0.13f + bite * 0.48f;
            head.getChild("jaw_r").xRot = 0.13f + bite * 0.48f;
            head.getChild("throat").xRot = Mth.sin(state.ageInTicks * 0.13f) * 0.09f;
            root.getChild("abdomen").xRot = Mth.sin(state.ageInTicks * 0.065f) * 0.05f;
            for (int i = 0; i < legs.length; i++) {
                boolean front = i < 2;
                float phase = i == 0 || i == 3 ? 0 : (float) Math.PI;
                float stride = state.walkAnimationPos * 0.78f + phase;
                legs[i].xRot = (front ? -0.55f : 0.64f) + Mth.cos(stride) * state.walkAnimationSpeed * 0.48f;
                legs[i].getChild("lower").xRot = (front ? 0.85f : -1.08f)
                        + Mth.sin(stride) * state.walkAnimationSpeed * 0.28f - (front ? bite * 0.3f : 0);
            }
        }
    }
}