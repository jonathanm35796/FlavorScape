package io.ruin.model.entity.npc.actions.edgeville;

import io.ruin.model.entity.npc.NPCAction;
import io.ruin.model.entity.shared.listeners.SpawnListener;
import io.ruin.model.map.Position;
import io.ruin.model.shop.PvPMarket;

public class PvPShop {
    static {
        SpawnListener.register(PvPMarket.NPC_ID,npc->{
            npc.walkTo=new Position(3096,3506,0);
            npc.skipReachCheck=p->p.equals(3096,3506,0)||p.equals(3096,3507,0);
        });
        NPCAction.register(PvPMarket.NPC_ID,"Trade",(player,npc)->PvPMarket.open(player));
    }
}
