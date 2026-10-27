package io.ruin.model.combat;

import io.ruin.model.activities.wilderness.BountyHunter;
import io.ruin.model.entity.player.*;
import io.ruin.model.item.Item;
import io.ruin.model.stat.*;
import io.ruin.model.skills.prayer.Prayer;
import java.util.function.Consumer;
import org.junit.*;
import static org.junit.Assert.*;

public class CombatLevelEditorTest {
    @BeforeClass public static void data()throws Exception{
        CombatEquipmentInteractionTest.data();
        java.lang.reflect.Field stage=io.ruin.process.CoreWorker.class.getDeclaredField("processStage");stage.setAccessible(true);
        stage.set(null,io.ruin.process.CoreWorker.Stage.UPDATE);
    }
    private static class EditorPlayer extends CombatEquipmentInteractionTest.Fighter {
        boolean attacking,defending,dead,locked;
        Consumer<Integer> input;
        int openedInterface=-1;
        final BountyHunter bh=new BountyHunter();
        GameMode mode=GameMode.STANDARD;
        final PlayerCombat editingCombat=new PlayerCombat(){
            @Override public boolean isAttacking(int ticks){return attacking;}
            @Override public boolean isDefending(long ticks){return defending;}
            @Override public boolean isDead(){return dead;}
        };
        EditorPlayer(){wildernessLevel=0;bh.init(this);editingCombat.init(this);}
        @Override public PlayerCombat getCombat(){return editingCombat==null?super.getCombat():editingCombat;}
        @Override public BountyHunter getBountyHunter(){return bh;}
        @Override public boolean isLocked(){return locked;}
        @Override public int getHeight(){return 0;}
        @Override public boolean isAdmin(){return false;}
        @Override public GameMode getGameMode(){return mode;}
        @Override public void integerInput(String prompt,Consumer<Integer> consumer){input=consumer;}
        @Override public void openInterface(io.ruin.model.inter.InterfaceType type,int id){openedInterface=id;}
    }
    @Test public void ordinaryAccountCanRaiseAndLowerEveryCombatSkill(){
        EditorPlayer p=new EditorPlayer();
        for(StatType type:StatType.values())if(type.isCombat()){
            assertTrue(CombatLevelEditor.set(p,type,99));
            assertEquals(Stat.xpForLevel(99),p.getStats().get(type).experience,0);
            int min=type==StatType.Hitpoints?10:1;
            assertTrue(CombatLevelEditor.set(p,type,min));
            assertEquals(min,p.getStats().get(type).fixedLevel);assertEquals(min,p.getStats().get(type).currentLevel);
            assertEquals(Stat.xpForLevel(min),p.getStats().get(type).experience,0);
        }
        assertEquals(99,p.getStats().get(StatType.Slayer).fixedLevel);
    }
    @Test public void ironAccountsCanChooseCombatBuildsToo(){
        EditorPlayer p=new EditorPlayer();p.mode=GameMode.IRONMAN;
        assertTrue(CombatLevelEditor.set(p,StatType.Defence,1));assertTrue(CombatLevelEditor.set(p,StatType.Prayer,52));
    }
    @Test public void noncombatAndOutOfRangeRequestsDoNotMutateStats(){
        EditorPlayer p=new EditorPlayer();
        assertFalse(CombatLevelEditor.set(p,StatType.Mining,1));assertFalse(CombatLevelEditor.set(p,null,99));
        assertFalse(CombatLevelEditor.set(p,StatType.Attack,100));assertFalse(CombatLevelEditor.set(p,StatType.Attack,0));
        assertFalse(CombatLevelEditor.set(p,StatType.Hitpoints,9));assertEquals(99,p.getStats().get(StatType.Attack).fixedLevel);
    }
    @Test public void submittedDialogueRechecksCombatAndWilderness(){
        EditorPlayer p=new EditorPlayer();CombatLevelEditor.open(p,StatType.Attack);assertNotNull(p.input);
        p.attacking=true;p.input.accept(1);assertEquals(99,p.getStats().get(StatType.Attack).fixedLevel);
        p.attacking=false;p.wildernessLevel=1;p.input.accept(1);assertEquals(99,p.getStats().get(StatType.Attack).fixedLevel);
    }
    @Test public void defendingDeadLockedAndAssignedTargetsBlockEdits(){
        EditorPlayer p=new EditorPlayer();p.defending=true;assertFalse(CombatLevelEditor.set(p,StatType.Magic,1));
        p.defending=false;p.dead=true;assertFalse(CombatLevelEditor.set(p,StatType.Magic,1));
        p.dead=false;p.locked=true;assertFalse(CombatLevelEditor.set(p,StatType.Magic,1));
        p.locked=false;p.bh.target=new EditorPlayer();assertFalse(CombatLevelEditor.set(p,StatType.Magic,1));
    }
    @Test public void tournamentAndDuelNegotiationsBlockEdits(){
        EditorPlayer p=new EditorPlayer();p.joinedTournament=true;assertFalse(CombatLevelEditor.set(p,StatType.Strength,1));
        p.joinedTournament=false;
        io.ruin.model.activities.duelarena.Duel duel=new io.ruin.model.activities.duelarena.Duel(p);duel.stage=1;
        EditorPlayer duelling=new EditorPlayer(){@Override public io.ruin.model.activities.duelarena.Duel getDuel(){return duel;}};
        assertFalse(CombatLevelEditor.set(duelling,StatType.Strength,1));
    }
    @Test public void loweringBelowWornGearRequirementsIsRejected(){
        EditorPlayer p=new EditorPlayer();p.equipment.set(3,new Item(4151));
        assertFalse(CombatLevelEditor.set(p,StatType.Attack,1));assertEquals(99,p.getStats().get(StatType.Attack).fixedLevel);
        p.equipment.set(3,null);assertTrue(CombatLevelEditor.set(p,StatType.Attack,1));
    }
    @Test public void editingRecalculatesCombatLevelAndDeactivatesPrayers(){
        EditorPlayer p=new EditorPlayer();p.prayer.toggle(Prayer.PIETY);assertTrue(p.prayer.isActive(Prayer.PIETY));
        assertTrue(CombatLevelEditor.set(p,StatType.Prayer,1));assertFalse(p.prayer.isActive(Prayer.PIETY));
        assertEquals(CombatUtils.getCombatLevel(p),p.getCombat().getLevel());
    }
    @Test public void experienceRoundTripRestoresTheSelectedLevel(){
        EditorPlayer p=new EditorPlayer();assertTrue(CombatLevelEditor.set(p,StatType.Defence,42));
        com.google.gson.Gson gson=new com.google.gson.GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        Stat saved=gson.fromJson(gson.toJson(p.getStats().get(StatType.Defence)),Stat.class);
        assertEquals(42,Stat.levelForXp(saved.experience));assertEquals(42,saved.currentLevel);
    }
    @Test public void skillsTabOffersEditingAndRetainsGuides()throws Exception{
        Class.forName("io.ruin.model.inter.handlers.TabStats");EditorPlayer p=new EditorPlayer();
        io.ruin.model.inter.actions.OptionAction attack=(io.ruin.model.inter.actions.OptionAction)
                io.ruin.model.inter.InterfaceHandler.HANDLERS[320].actions[1];
        attack.handle(p,1);assertNotNull(p.input);
        attack.handle(p,2);assertEquals(io.ruin.model.inter.Interface.SKILL_GUIDE,p.openedInterface);
        io.ruin.model.inter.actions.OptionAction mining=(io.ruin.model.inter.actions.OptionAction)
                io.ruin.model.inter.InterfaceHandler.HANDLERS[320].actions[17];
        p.input=null;mining.handle(p,1);assertNull(p.input);
    }
}
