@echo off
REM    EcoPos Touch Friendly Point of Sales designed for Touch Screen
REM    Copyright (c) 2009-2014 EcoPos & previous Openbravo POS works
REM    http://sourceforge.net/projects/ecopos
REM
REM    This file is part of EcoPos
REM    Contributed by John L
REM
REM    EcoPos is free software: you can redistribute it and/or modify
REM    it under the terms of the GNU General Public License as published by
REM    the Free Software Foundation, either version 3 of the License, or
REM    (at your option) any later version.
REM
REM    EcoPos is distributed in the hope that it will be useful,
REM    but WITHOUT ANY WARRANTY; without even the implied warranty of
REM    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM    GNU General Public License for more details.
REM
REM    You should have received a copy of the GNU General Public License
REM    along with EcoPos.  If not, see <http://www.gnu.org/licenses/>
REM
set DIRNAME=%~dp0
set CP="%DIRNAME%ecopos.jar"
set CP=%CP%;"%DIRNAME%locales/"
set CP=%CP%;"%DIRNAME%lib/substance.jar"
start /B javaw -cp %CP% com.openbravo.pos.sales.JResetPickupID