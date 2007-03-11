/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.springframework.ide.eclipse.beans.ui.graph.figures;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.swt.graphics.Color;

/**
 * Draws a rectangle border with a shadow.
 */
public class ShadowedLineBorder extends LineBorder {

	private int shadowWidth;

	public ShadowedLineBorder() {
		this(ColorConstants.black, 1, 4);
	}

	public ShadowedLineBorder(Color color, int lineWidth, int shadowWidth) {
		super(color, lineWidth);
		this.shadowWidth = shadowWidth;
	}

	/**
	 * Sets the width of the shadow.
	 *
	 * @param width  the width of the shadow
	 */
	public void setShadowWidth(int shadowWidth) {
		this.shadowWidth = shadowWidth;
	}

	/**
	 * Return the width of the shadow.
	 * @return width of the shadow
	 */
	public int getShadowWidth() {
		return this.shadowWidth;
	}

	/**
	 * Returns the space used by the border for the figure provided as input.
	 * @param figure The figure this border belongs to
	 * @return This border's insets
	 */
	@Override
	public Insets getInsets(IFigure figure) {
		return new Insets(getWidth(), getWidth(), getWidth() + getShadowWidth(),
						  getWidth() + getShadowWidth());
	}

	@Override
	public void paint(IFigure figure, Graphics graphics, Insets insets) {

		// Paint line border [copied from super.paint()]
		tempRect.setBounds(getPaintRectangle(figure, insets));
		tempRect.width -= getShadowWidth();
		tempRect.height -= getShadowWidth();
		if (getWidth() % 2 == 1) {
			tempRect.width--;
			tempRect.height--;
		}
		tempRect.shrink(getWidth() / 2, getWidth() / 2);
		graphics.setLineWidth(getWidth());
		if (getColor() != null) {
			graphics.setForegroundColor(getColor());
		}
		graphics.drawRectangle(tempRect);

		// Paint the shadow by reusing the temporary rectangle already
		// initialized by super.paint()
		PointList plt = new PointList();
		plt.addPoint(tempRect.x + 1 + tempRect.width,
					 tempRect.y + getShadowWidth());
		plt.addPoint(tempRect.x + tempRect.width + 1,
					 tempRect.y + tempRect.height + 1);
		plt.addPoint(tempRect.x + getShadowWidth(),
					 tempRect.y + tempRect.height + 1);
		plt.addPoint(tempRect.x + getShadowWidth(),
					 tempRect.y + tempRect.height + getShadowWidth());
		plt.addPoint(tempRect.x + tempRect.width + getShadowWidth(),
					 tempRect.y + tempRect.height + getShadowWidth());
		plt.addPoint(tempRect.x + tempRect.width + getShadowWidth(),
					 tempRect.y + getShadowWidth());
		graphics.setBackgroundColor(ColorConstants.lightGray);
		graphics.fillPolygon(plt);
	}
}
