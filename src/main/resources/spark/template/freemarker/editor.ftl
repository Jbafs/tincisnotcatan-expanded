<#assign content>

<style>
  html, body { height: 100%; margin: 0; overflow: hidden; }
  #editor-outer { display: flex; height: 100vh; }

  /* Left toolbar */
  #left-panel {
    width: 148px; min-width: 148px;
    background: #24243a; color: #ddd;
    padding: 10px 8px; overflow-y: auto;
    flex-shrink: 0;
  }
  #left-panel h6 { color: #aaa; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; margin: 0 0 6px; }
  #left-panel hr { border-color: #444; margin: 8px 0; }
  #left-panel label { font-size: 12px; color: #bbb; margin: 0 0 3px; display: block; }
  #left-panel .form-control { font-size: 12px; padding: 3px 6px; height: 28px; }
  #left-panel p { font-size: 11px; color: #666; margin: 0; }

  .tile-type-btn {
    display: block; width: 100%; margin-bottom: 3px;
    padding: 5px 8px; border: 2px solid transparent; border-radius: 3px;
    cursor: pointer; font-size: 12px; font-weight: 600;
    text-align: left; color: #fff;
  }
  .tile-type-btn.active { border-color: #fff; box-shadow: 0 0 4px rgba(255,255,255,0.4); }
  .tile-type-btn:hover { opacity: 0.9; }

  .btn-BRICK      { background: #c06020; }
  .btn-WOOD       { background: #1a8020; }
  .btn-ORE        { background: #7888a0; }
  .btn-WHEAT      { background: #c8a810; color: #333; }
  .btn-SHEEP      { background: #60b830; color: #333; }
  .btn-DESERT     { background: #c09840; color: #333; }
  .btn-GOLD_FIELD { background: #e0a800; color: #333; }
  .btn-SEA        { background: #3878b8; }
  .btn-EMPTY      { background: #555; }

  /* SVG canvas */
  #svg-container { flex: 1; overflow: hidden; position: relative; background: #c8dcea; }
  #editor-svg { width: 100%; height: 100%; display: block; cursor: crosshair; }

  /* Right panel */
  #right-panel {
    width: 190px; min-width: 190px;
    background: #f7f7f7; border-left: 1px solid #ddd;
    padding: 10px; overflow-y: auto;
    flex-shrink: 0; font-size: 13px;
  }
  #right-panel h5 { font-size: 13px; font-weight: 700; margin: 0 0 8px; border-bottom: 1px solid #ddd; padding-bottom: 4px; }
  #right-panel .form-group { margin-bottom: 7px; }
  #right-panel label { font-size: 12px; margin-bottom: 2px; display: block; }
  #right-panel .form-control { font-size: 12px; padding: 3px 6px; height: 28px; }
  #right-panel .btn { font-size: 12px; }
  #right-panel hr { margin: 8px 0; }
</style>

<div id="editor-outer">

  <!-- Left: Tile Palette -->
  <div id="left-panel">
    <h6>Tile Type</h6>
    <button class="tile-type-btn btn-BRICK  active" data-type="BRICK">Brick</button>
    <button class="tile-type-btn btn-WOOD"         data-type="WOOD">Wood</button>
    <button class="tile-type-btn btn-ORE"          data-type="ORE">Ore</button>
    <button class="tile-type-btn btn-WHEAT"        data-type="WHEAT">Wheat</button>
    <button class="tile-type-btn btn-SHEEP"        data-type="SHEEP">Sheep</button>
    <button class="tile-type-btn btn-DESERT"       data-type="DESERT">Desert</button>
    <button class="tile-type-btn btn-GOLD_FIELD"   data-type="GOLD_FIELD">Gold Field</button>
    <button class="tile-type-btn btn-SEA"          data-type="SEA">Sea</button>
    <button class="tile-type-btn btn-EMPTY"        data-type="EMPTY">Erase</button>

    <hr>

    <div id="roll-group">
      <label>Roll Number</label>
      <select id="roll-select" class="form-control">
        <option value="2">2</option>
        <option value="3">3</option>
        <option value="4">4</option>
        <option value="5">5</option>
        <option value="6" selected>6</option>
        <option value="8">8</option>
        <option value="9">9</option>
        <option value="10">10</option>
        <option value="11">11</option>
        <option value="12">12</option>
      </select>
    </div>

    <div id="port-group" style="display:none">
      <label>Port</label>
      <select id="port-select" class="form-control">
        <option value="none">None</option>
        <option value="WILDCARD">3:1 Wildcard</option>
        <option value="BRICK">2:1 Brick</option>
        <option value="WOOD">2:1 Wood</option>
        <option value="ORE">2:1 Ore</option>
        <option value="WHEAT">2:1 Wheat</option>
        <option value="SHEEP">2:1 Sheep</option>
      </select>
    </div>

    <hr>
    <p>Click: paint<br>Right-click: erase<br>Scroll: zoom<br>Middle-drag: pan</p>
  </div>

  <!-- Center: SVG Canvas -->
  <div id="svg-container">
    <svg id="editor-svg" xmlns="http://www.w3.org/2000/svg"></svg>
  </div>

  <!-- Right: Board Settings + Play -->
  <div id="right-panel">
    <h5>Board Settings</h5>
    <div class="form-group">
      <label>Board Name</label>
      <input id="board-name" class="form-control" placeholder="My Custom Board">
    </div>
    <div class="form-group">
      <label>Suggested Players</label>
      <select id="board-players" class="form-control">
        <option value="2">2</option>
        <option value="3">3</option>
        <option value="4" selected>4</option>
        <option value="5">5</option>
        <option value="6">6</option>
      </select>
    </div>
    <div class="form-group">
      <label>Suggested VP</label>
      <input id="board-vp" class="form-control" type="number" value="10" min="5" max="20">
    </div>
    <button class="btn btn-default btn-block" onclick="showExportModal()">Export JSON</button>

    <hr>

    <h5>Play This Board</h5>
    <div class="form-group">
      <label>Your Name</label>
      <input id="play-username" class="form-control" placeholder="Username">
    </div>
    <div class="form-group">
      <label>Game Name</label>
      <input id="play-game-name" class="form-control" placeholder="Custom Game">
    </div>
    <div class="form-group">
      <label>Players</label>
      <select id="play-players" class="form-control">
        <option value="2">2</option>
        <option value="3">3</option>
        <option value="4" selected>4</option>
        <option value="5">5</option>
        <option value="6">6</option>
      </select>
    </div>
    <div class="form-group">
      <label>Victory Points</label>
      <input id="play-vp" class="form-control" type="number" value="10" min="5" max="20">
    </div>
    <div class="checkbox" style="margin:4px 0 8px">
      <label style="font-size:12px">
        <input type="checkbox" id="play-seafarers" checked> Seafarers rules (ships)
      </label>
    </div>
    <button class="btn btn-success btn-block" onclick="startGameWithBoard()">Start Game</button>
    <hr>
    <a href="/home" class="btn btn-link btn-block" style="font-size:12px">&larr; Back to Home</a>
  </div>
</div>

<!-- Export JSON Modal -->
<div class="modal fade" id="export-modal" tabindex="-1" role="dialog">
  <div class="modal-dialog modal-lg" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h4 class="modal-title">Export Board JSON</h4>
      </div>
      <div class="modal-body">
        <p class="text-muted" style="font-size:12px">
          Save this as <code>src/main/resources/scenarios/your_name.json</code> to use as a permanent scenario,
          or use "Play This Board" to play it once directly.
        </p>
        <textarea id="export-json-text" class="form-control" rows="16" readonly style="font-family:monospace;font-size:12px"></textarea>
      </div>
      <div class="modal-footer">
        <button id="copy-json-btn" class="btn btn-default" onclick="copyJSON()">Copy</button>
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>

</#assign>
<#include "main.ftl">
<script src="js/editor.js"></script>
