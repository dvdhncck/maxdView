
public void doIt()
{
  ExprData.TagAttrs ta = edata.getSpotTagAttrs();
 
  int col = 1;
  int row = 1;

  for(int s=0; s < edata.getNumSpots(); s++)
  {
    ta.setTagAttr(edata.getSpotName(s), "ROW", String.valueOf(row));
    ta.setTagAttr(edata.getSpotName(s), "COL", String.valueOf(col));
    if(++col > 12)
    { col = 1;
      row++;
    }

  }
}
