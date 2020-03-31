/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.client.ui.htmlframe;

import java.awt.*;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.TooManyListenersException;
import javax.swing.*;
import javax.swing.text.*;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.ScreenPoint;
import net.rptools.maptool.client.TransferableHelper;
import net.rptools.maptool.client.ui.zone.ZoneRenderer;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.model.ZonePoint;

/** Represents the transparent HTML overlay over the map. */
public class HTMLOverlay extends HTMLPane implements DropTargetListener {
  /** The default rule for an invisible body tag. */
  private static final String CSS_RULE_BODY =
      "body { font-family: sans-serif; font-size: %dpt; background: none}";

  public HTMLOverlay() {
    super();
    setFocusable(false);
    setHighlighter(null);
    setOpaque(false);
    addMouseListeners();
    setCaretColor(new Color(0, 0, 0, 0)); // invisible, needed or it shows in DnD operations

    setTransferHandler(new TransferableHelper()); // set the Drag & Drop handler
    try {
      getDropTarget().addDropTargetListener(this);
    } catch (TooManyListenersException e1) {
      // Should never happen because the transfer handler fixes this problem.
    }
  }

  /**
   * Return the rule for an invisible body.
   *
   * @return the rule
   */
  @Override
  public String getRuleBody() {
    return String.format(CSS_RULE_BODY, AppPreferences.getFontSize());
  }

  @Override
  public void dragEnter(DropTargetDragEvent dtde) {}

  @Override
  public void dragOver(DropTargetDragEvent dtde) {}

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {}

  @Override
  public void dragExit(DropTargetEvent dte) {}

  /**
   * Add the tokens to the current zone renderer if a token is dropped on the overlay.
   *
   * @param dtde the event of the drop
   */
  @Override
  public void drop(DropTargetDropEvent dtde) {
    ZoneRenderer zr = MapTool.getFrame().getCurrentZoneRenderer();
    Point point = SwingUtilities.convertPoint(this, dtde.getLocation(), zr);

    ZonePoint zp = new ScreenPoint((int) point.getX(), (int) point.getY()).convertToZone(zr);
    TransferableHelper th = (TransferableHelper) getTransferHandler();
    List<Token> tokens = th.getTokens();
    if (tokens != null && !tokens.isEmpty()) {
      zr.addTokens(tokens, zp, th.getConfigureTokens(), false);
    }
  }

  /** Add the mouse listeners to forward the mouse events to the current ZoneRenderer. */
  private void addMouseListeners() {
    addMouseWheelListener(this::passMouseEvent);
    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            passMouseEvent(e);
          }

          @Override
          public void mouseDragged(MouseEvent e) {
            passMouseEvent(e);
          }
        });
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            passMouseEvent(e, true);
          }

          @Override
          public void mouseClicked(MouseEvent e) {
            passMouseEvent(e, true);
          }

          @Override
          public void mouseEntered(MouseEvent e) {
            passMouseEvent(e);
          }

          @Override
          public void mouseReleased(MouseEvent e) {
            passMouseEvent(e);
          }

          @Override
          public void mouseExited(MouseEvent e) {
            passMouseEvent(e);
          }
        });
  }

  /**
   * Passes a mouse event to the ZoneRenderer. If checking for transparency, only forwards the event
   * if it happened over a transparent pixel.
   *
   * @param e the mouse event to forward
   * @param checkForTransparency whether to check for transparency
   */
  private void passMouseEvent(MouseEvent e, boolean checkForTransparency) {
    SwingUtilities.invokeLater(
        () -> {
          if (checkForTransparency && isOpaque(e.getPoint())) {
            return; // don't forward
          }
          Component c = MapTool.getFrame().getCurrentZoneRenderer();
          c.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, c));
        });
  }

  /**
   * Passes a mouse event to the ZoneRenderer.
   *
   * @param e the mouse event to forward
   */
  private void passMouseEvent(MouseEvent e) {
    passMouseEvent(e, false);
  }

  /**
   * Returns true if the pixel of the component at the point is opaque.
   *
   * @param p the point
   * @return true if the pixel is opaque
   */
  public boolean isOpaque(Point p) {
    Rectangle rect = getBounds();
    BufferedImage img = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
    paintAll(img.createGraphics());
    return new Color(img.getRGB(p.x, p.y), true).getAlpha() != 0;
  }
}