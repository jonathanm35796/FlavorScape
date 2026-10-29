package net.runelite.client.plugins.lletyamarket;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.standalone.LletyaMarketState;

/** A single scroll document containing category headers and their item grids. */
public final class MarketLayout {
    public static final int HEADER_HEIGHT=24, CELL=43, COLUMNS=8;
    public static final class Section {
        public final int category,y,count;
        Section(int category,int y,int count){this.category=category;this.y=y;this.count=count;}
    }
    public static final class Cell {
        public final int itemIndex;
        public final Rectangle bounds;
        Cell(int index,int x,int y){itemIndex=index;bounds=new Rectangle(x,y,CELL,CELL);}
    }
    public final List<Section> sections;
    public final List<Cell> cells;
    public final int height;
    public MarketLayout(List<LletyaMarketState.Item> items,boolean includeEmpty) {
        List<Section> sections=new ArrayList<>();List<Cell> cells=new ArrayList<>();int y=0;
        for(int category=0;category<9;category++){
            List<Integer> group=new ArrayList<>();
            for(int i=0;i<items.size();i++)if(items.get(i).category==category)group.add(i);
            if(group.isEmpty()&&!includeEmpty)continue;
            sections.add(new Section(category,y,group.size()));
            for(int i=0;i<group.size();i++)cells.add(new Cell(group.get(i),(i%COLUMNS)*CELL+1,y+HEADER_HEIGHT+(i/COLUMNS)*CELL));
            y+=HEADER_HEIGHT+(group.isEmpty()?26:((group.size()+COLUMNS-1)/COLUMNS)*CELL)+8;
        }
        this.sections=Collections.unmodifiableList(sections);this.cells=Collections.unmodifiableList(cells);height=y;
    }
    public int maximumScroll(int viewportHeight){return Math.max(0,height-viewportHeight);}
    public int headerOffset(int category){for(Section s:sections)if(s.category==category)return s.y;return -1;}
    public int categoryAt(int scroll){int category=sections.isEmpty()?-1:sections.get(0).category;for(Section s:sections)if(s.y<=scroll)category=s.category;return category;}
    public int itemAt(int x,int y){for(Cell c:cells)if(c.bounds.contains(x,y))return c.itemIndex;return -1;}
}
