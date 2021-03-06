/*
 * Copyright (C),2014-2018 Andrew John Jacobs.
 *
 * This program is provided free of charge for educational purposes
 *
 * Redistribution and use in binary form without modification, is permitted
 * provided that the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS 'AS IS' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.me.obelisk.xide.swing;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import com.javadocking.dockable.DefaultDockable;

public abstract class DockablePanel extends Window
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void show ()
	{
		contentPanel.setVisible (true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void hide ()
	{
		contentPanel.setVisible (false);
	}
	
	protected DefaultDockable	dockable;
	
	/**
	 * The <CODE>JPanel</CODE> containing the main GUI components.
	 */
	protected JPanel			contentPanel
		= new JPanel ();

	/**
	 */
	protected DockablePanel (final String id, final String filename)
	{
		super (filename);
		
		contentPanel.setLayout(new BorderLayout ());
		
		dockable = new DefaultDockable (id, contentPanel, getString ("title"), getIcon ("icon"));
		dockable.setDescription (getString ("description"));
	}
}