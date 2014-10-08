package org.exoplatform.chat.model;

import java.util.ArrayList;
import java.util.List;

public class SpaceBeans implements java.io.Serializable {

  ArrayList<SpaceBean> spaces_;

  public SpaceBeans(ArrayList<SpaceBean> spaces)
  {
    spaces_ = spaces;
  }

  public List<SpaceBean> getSpaces() {
    return spaces_;
  }

  public void setSpaces(ArrayList<SpaceBean> spaces) {
    this.spaces_ = spaces;
  }
}
