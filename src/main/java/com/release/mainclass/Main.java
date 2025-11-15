package com.release.mainclass;

import java.util.Random;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.BossInfo;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;


@Mod("RWE")
public class Main {

    private int cooldown = 30;
    private int tickCounter = 0;

    private final ServerBossInfo bossBar = new ServerBossInfo(
            new StringTextComponent("Next event"),
            BossInfo.Color.RED,
            BossInfo.Overlay.PROGRESS
    );

    public Main() {
        MinecraftForge.EVENT_BUS.register(this);
        bossBar.setVisible(true);
    }


    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;

        int totalTicks = 20 * cooldown; 

        if (tickCounter >= totalTicks) {
            tickCounter = 0;
            runMyTask();
        }

        updateBossBar(totalTicks);
    }

    private void updateBossBar(int totalTicks) {
        int remainingTicks = totalTicks - tickCounter;
        if (remainingTicks < 0) remainingTicks = 0;

        float progress = totalTicks > 0 ? (float) remainingTicks / (float) totalTicks : 0.0F;
        bossBar.setPercent(progress);

        int secondsLeft = remainingTicks / 20;

        bossBar.setName(new StringTextComponent("Next event: " + secondsLeft + "s"));
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            bossBar.addPlayer((ServerPlayerEntity) event.getPlayer());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            bossBar.removePlayer((ServerPlayerEntity) event.getPlayer());
        }
    }
	
	public static void scheduleTask(MinecraftServer server, int delayTicks, Runnable task) {
		MinecraftForge.EVENT_BUS.register(new Object() {
			int ticks = 0;

			@SubscribeEvent
			public void onTick(TickEvent.ServerTickEvent event) {
				if (event.phase == TickEvent.Phase.END) {
					ticks++;
					if (ticks >= delayTicks) {
						task.run();
						MinecraftForge.EVENT_BUS.unregister(this);
					}
				}
			}
		});
	}

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("event")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> {
                    runMyTask();
                    tickCounter = 0;
                    updateBossBar(20 * cooldown);
                    return 1;
                })
        );

        dispatcher.register(
            Commands.literal("settimer")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int sec = IntegerArgumentType.getInteger(ctx, "seconds");

                        cooldown = sec;  
                        tickCounter = 0;  

                        ctx.getSource().sendSuccess(
                            new StringTextComponent("The interval between events is set to " + sec + " second(s)."),
                            true
                        );

                        updateBossBar(20 * cooldown);

                        return 1;
                    })
                )
        );
    }
void runMyTask() {
        Random rand = new Random();
        int randomEvent = rand.nextInt(15) + 1;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            switch (randomEvent) {
                case 1:
                    player.sendMessage(new StringTextComponent("Diamond!"), player.getUUID());
                    player.addItem(new ItemStack(Items.DIAMOND, 1));
                    break;

                case 2:
                    player.sendMessage(new StringTextComponent("TNT D:"), player.getUUID());
                    if (!player.level.isClientSide) {
                        for (int i = 0; i < 3; i++) {
                            double x = player.getX();
                            double y = player.getY() + player.getBbHeight() + 5;
                            double z = player.getZ();

                            TNTEntity tnt = new TNTEntity(EntityType.TNT, player.level);
                            tnt.setPos(x, y, z);
                            tnt.setFuse(60);
                            player.level.addFreshEntity(tnt);
                        }
                    }
                    break;

                case 3:
                    player.sendMessage(new StringTextComponent("charge!"), player.getUUID());
                    if (!player.level.isClientSide) {
                        ServerWorld world = (ServerWorld) player.level;

                        double x = player.getX();
                        double y = player.getY() + 2.0;
                        double z = player.getZ();

                        LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
                        if (lightning != null) {
                            lightning.moveTo(x, y, z);
                            lightning.setVisualOnly(false);
                            lightning.setCause(player);
                            world.addFreshEntity(lightning);
                        }
                    }
                    break;

                case 4: {
                    player.sendMessage(new StringTextComponent("shhhhhh.."), player.getUUID());
                    ServerWorld world = (ServerWorld) player.level;

                    for (int i = 0; i < 3; i++) {
                        double x = player.getX() + (world.random.nextDouble() - 0.5) * 2;
                        double y = player.getY();
                        double z = player.getZ() + (world.random.nextDouble() - 0.5) * 2;

                        CreeperEntity crp = EntityType.CREEPER.create(world);
                        if (crp != null) {
                            crp.moveTo(x, y, z);
                            world.addFreshEntity(crp);
                        }
                    }
                    break;
                }

                case 5: {
                    player.sendMessage(new StringTextComponent("investing"), player.getUUID());

                    player.setItemInHand(player.getUsedItemHand(), ItemStack.EMPTY);

                    ItemStack dirtStack = new ItemStack(Blocks.DIRT, 64);

                    player.addItem(dirtStack.copy());
                    player.addItem(dirtStack.copy());
                    player.addItem(dirtStack.copy());
                    break;
                }

                case 6: {
                    player.sendMessage(new StringTextComponent("mi bombo :D"), player.getUUID());

                    if (!player.level.isClientSide) {				
                        ServerWorld world = (ServerWorld) player.level;

                        for (int i = 0; i < 3; i++) {
                            double x = player.getX() + (world.random.nextDouble() - 0.5) * 2;
                            double y = player.getY();
                            double z = player.getZ() + (world.random.nextDouble() - 0.5) * 2;

                            CreeperEntity crp = EntityType.CREEPER.create(world);
                            if (crp != null) {
                                crp.moveTo(x, y, z, world.random.nextFloat() * 360F, 0F);
                                world.addFreshEntity(crp);
                            }
                        }

                        double lx = player.getX();
                        double ly = player.getY() + 2.0;
                        double lz = player.getZ();

                        LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
                        if (lightning != null) {
                            lightning.moveTo(lx, ly, lz);
                            lightning.setVisualOnly(false);
                            lightning.setCause(player);
                            world.addFreshEntity(lightning);
                        }
                    }
                    break;
                }
				case 7: {
					player.sendMessage(new StringTextComponent("blindness"), player.getUUID());
					player.addEffect(new EffectInstance(Effects.BLINDNESS, 30 * 20, 0)); 
					break;
				}
				case 8: {
					player.sendMessage(new StringTextComponent("hunger strike"), player.getUUID());
					player.addEffect(new EffectInstance(Effects.HUNGER, 1 * 20, 255)); 
					break;
				}
				case 9: {
					player.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5 * 20, 255)); 
					player.addEffect(new EffectInstance(Effects.JUMP, 5 * 20, 129));
					player.sendMessage(new StringTextComponent("STOP!"), player.getUUID());
					break;
				}
				case 10: {

					player.sendMessage(new StringTextComponent("surprise! :D"), player.getUUID());

					if (!player.level.isClientSide) {

						ChickenEntity chicken = EntityType.CHICKEN.create(player.level);
						if (chicken != null) {
							chicken.setPos(player.getX(), player.getY(), player.getZ());

							chicken.addEffect(new EffectInstance(Effects.GLOWING, 20 * 60, 0));
							player.level.addFreshEntity(chicken);

							scheduleTask(player.level.getServer(), 100, () -> {

								if (!chicken.isAlive()) return;

								Random rand1 = new Random();
								boolean dropDiamonds = rand1.nextBoolean();

								if (dropDiamonds) {

									ItemEntity diamonds = new ItemEntity(
											player.level,
											chicken.getX(), chicken.getY(), chicken.getZ(),
											new ItemStack(Items.DIAMOND, 5)
									);
									player.level.addFreshEntity(diamonds);

								} else {

									TNTEntity bomboclad = new TNTEntity(EntityType.TNT, player.level);
									bomboclad.setPos(chicken.getX(), chicken.getY(), chicken.getZ());
									bomboclad.setFuse(1);
									player.level.addFreshEntity(bomboclad);
								}
							});
						}
					}
					break;
				}
				case 11: {
					player.sendMessage(new StringTextComponent("oh... (a bucket of water in your inventory)"), player.getUUID());

						if (!player.level.isClientSide()) {
							player.addEffect(new EffectInstance(Effects.LEVITATION, 1 * 20, 100));
							ItemStack water = new ItemStack(Items.WATER_BUCKET);
							player.addItem(water.copy());
						}

					break;
				}
				case 12: {
					player.sendMessage(new StringTextComponent("get buffed!"), player.getUUID());
					if (!player.level.isClientSide()) {
						player.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 10 * 20, 0));
						player.addEffect(new EffectInstance(Effects.REGENERATION, 10 * 20, 0));
						player.addEffect(new EffectInstance(Effects.DIG_SPEED, 10 * 20, 0));
					}
					break;
				}
				case 13: {
					player.sendMessage(new StringTextComponent("kit"), player.getUUID());
					ItemStack iron = new ItemStack(Items.IRON_INGOT, 3);
					ItemStack xavalo = new ItemStack(Items.COOKED_BEEF, 2);
					ItemStack gold = new ItemStack(Items.GOLD_INGOT, 8);
					ItemStack apple = new ItemStack(Items.APPLE, 1);
					
					player.addItem(iron.copy());
					player.addItem(xavalo.copy());
					player.addItem(gold.copy());
					player.addItem(apple.copy());
					break;
				}
				case 14: {
					player.sendMessage(new StringTextComponent("small problem"), player.getUUID());
					if (!player.level.isClientSide) {
                        double x = player.getX();
                        double y = player.getY() + player.getBbHeight() + 5;
                        double z = player.getZ();
						   
                        ZombieEntity enemy = new ZombieEntity(EntityType.ZOMBIE, player.level);
                        enemy.setPos(x, y, z);
                        player.level.addFreshEntity(enemy);
						
                        SpiderEntity enemy2 = new SpiderEntity(EntityType.SPIDER, player.level);
                        enemy2.setPos(x, y, z);
                        player.level.addFreshEntity(enemy2);
					}
					break;
				}
				case 15: {
					player.sendMessage(new StringTextComponent("get DEbuffed D:"), player.getUUID());
					if (!player.level.isClientSide) {
						player.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 15 * 20, 0));
						player.addEffect(new EffectInstance(Effects.WEAKNESS, 10 * 20, 1));
					}
					break;
				}
                default:
                    break;
            }
        }
    }
}
