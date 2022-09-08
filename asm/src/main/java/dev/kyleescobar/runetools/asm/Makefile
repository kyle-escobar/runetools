# All files in the distribution of BLOAT (Bytecode Level Optimization and
# Analysis tool for Java(tm)) are Copyright 1997-2001 by the Purdue
# Research Foundation of Purdue University.  All rights reserved.
# 
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA


SUBDIRS = util \
	  reflect \
	  file \
	  editor \
	  inline \
	  tree \
	  cfg \
	  ssa \
	  tbaa \
	  trans \
	  diva \
	  codegen \
	  context \
	  decorate \
	  optimize \
	  shrink \
	  strip \
	  dump \
	  tools \
	  benchmark

all:
	@for i in $(SUBDIRS) ""; do \
	    if [ "x$$i" != "x" ]; then \
		$(MAKE) -C $$i all; \
	    fi; \
	done

clean:
	@for i in $(SUBDIRS) ""; do \
	    if [ "x$$i" != "x" ]; then \
		$(MAKE) -C $$i clean; \
	    fi; \
	done

docs:
	javadoc -d ../../../../../docs -sourcepath ../../../.. \
	  dev.kyleescobar.runetools.asm.util \
	  dev.kyleescobar.runetools.asm.reflect \
	  dev.kyleescobar.runetools.asm.file \
	  dev.kyleescobar.runetools.asm.editor \
	  dev.kyleescobar.runetools.asm.tree \
	  dev.kyleescobar.runetools.asm.cfg \
	  dev.kyleescobar.runetools.asm.ssa \
	  dev.kyleescobar.runetools.asm.tbaa \
	  dev.kyleescobar.runetools.asm.trans \
	  dev.kyleescobar.runetools.asm.diva \
	  dev.kyleescobar.runetools.asm.codegen \
	  dev.kyleescobar.runetools.asm.context \
	  dev.kyleescobar.runetools.asm.decorate \
	  dev.kyleescobar.runetools.asm.optimize \
	  dev.kyleescobar.runetools.asm.shrink \
	  dev.kyleescobar.runetools.asm.strip \
	  dev.kyleescobar.runetools.asm.dump \
	  dev.kyleescobar.runetools.asm.benchmark
