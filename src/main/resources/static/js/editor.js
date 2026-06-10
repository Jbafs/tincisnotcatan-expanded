// Map Editor for tincisnotcatan
// Coordinate system: cube coords (gx, gy, gz) using the game's BFS neighbor offsets.
// Cartesian: cx = sqrt(3)/2 * (gx-gz),  cy = 0.5*(gx+gz) - gy   (tile.js convention)

var HEX_NEIGHBORS = [
    [0,0,1], [0,1,1], [0,1,0], [1,1,0], [1,0,0], [1,0,1]
];

var TILE_COLORS = {
    BRICK:      '#c06020',
    WOOD:       '#1a8020',
    ORE:        '#7888a0',
    WHEAT:      '#c8a810',
    SHEEP:      '#60b830',
    DESERT:     '#c09840',
    GOLD_FIELD: '#e0a800',
    SEA:        '#3878b8'
};

var TILE_LABELS = {
    BRICK:'Brick', WOOD:'Wood', ORE:'Ore', WHEAT:'Wheat',
    SHEEP:'Sheep', DESERT:'Desert', GOLD_FIELD:'Gold', SEA:'Sea'
};

// ---- State ----
var tiles      = {};   // "gx,gy,gz" → {type, rollNum, portType}
var gridCells  = [];   // [{gx,gy,gz,cx,cy}]  (canonical grid positions)
var scale      = 60;
var offsetX    = 0, offsetY = 0;
var selectedType = 'BRICK';
var selectedRoll = 6;
var selectedPort = 'none';
var SVG_NS = 'http://www.w3.org/2000/svg';

// ---- Grid generation via BFS ----
function generateGrid(maxDepth) {
    var grid = [];
    var seenPos   = {};
    var seenCoord = {};
    var queue = [{gx:0, gy:0, gz:0, depth:0}];
    seenCoord['0,0,0'] = true;

    while (queue.length > 0) {
        var curr = queue.shift();
        var gx = curr.gx, gy = curr.gy, gz = curr.gz;

        var cx = (Math.sqrt(3) / 2) * (gx - gz);
        var cy = 0.5 * (gx + gz) - gy;
        var pk = Math.round(cx * 100) + ',' + Math.round(cy * 100);

        if (!seenPos[pk]) {
            seenPos[pk] = true;
            grid.push({gx:gx, gy:gy, gz:gz, cx:cx, cy:cy});
        }

        if (curr.depth < maxDepth) {
            for (var i = 0; i < 6; i++) {
                var nx = gx + HEX_NEIGHBORS[i][0];
                var ny = gy + HEX_NEIGHBORS[i][1];
                var nz = gz + HEX_NEIGHBORS[i][2];
                var nk = nx + ',' + ny + ',' + nz;
                if (!seenCoord[nk]) {
                    seenCoord[nk] = true;
                    queue.push({gx:nx, gy:ny, gz:nz, depth:curr.depth + 1});
                }
            }
        }
    }
    return grid;
}

// ---- SVG hex polygon points ----
function hexPoints(sx, sy) {
    var r = scale / Math.sqrt(3);
    var pts = [];
    for (var i = 0; i < 6; i++) {
        var a = Math.PI / 3 * i;
        pts.push((sx + r * Math.cos(a)).toFixed(1) + ',' + (sy + r * Math.sin(a)).toFixed(1));
    }
    return pts.join(' ');
}

// ---- Render ----
function render() {
    var svg = document.getElementById('editor-svg');
    var w = svg.clientWidth || 800;
    var h = svg.clientHeight || 600;
    svg.innerHTML = '';

    for (var i = 0; i < gridCells.length; i++) {
        var cell = gridCells[i];
        var key  = cell.gx + ',' + cell.gy + ',' + cell.gz;
        var tile = tiles[key];
        var sx   = offsetX + scale * cell.cx;
        var sy   = offsetY + scale * cell.cy;

        // Cull off-screen cells
        if (sx < -scale || sx > w + scale || sy < -scale || sy > h + scale) continue;

        var g = document.createElementNS(SVG_NS, 'g');

        // Hex polygon
        var poly = document.createElementNS(SVG_NS, 'polygon');
        poly.setAttribute('points', hexPoints(sx, sy));
        if (tile) {
            poly.setAttribute('fill',         TILE_COLORS[tile.type] || '#999');
            poly.setAttribute('stroke',       '#333');
            poly.setAttribute('stroke-width', '1.5');
        } else {
            poly.setAttribute('fill',         '#dde8f0');
            poly.setAttribute('fill-opacity', '0.55');
            poly.setAttribute('stroke',       '#b8c8d8');
            poly.setAttribute('stroke-width', '0.5');
        }
        g.appendChild(poly);

        if (tile) {
            // Roll number (land tiles only)
            if (tile.type !== 'SEA' && tile.type !== 'DESERT' && tile.rollNum > 0) {
                var t = document.createElementNS(SVG_NS, 'text');
                t.setAttribute('x',                  sx.toFixed(1));
                t.setAttribute('y',                  sy.toFixed(1));
                t.setAttribute('text-anchor',        'middle');
                t.setAttribute('dominant-baseline',  'middle');
                t.setAttribute('font-size',          (scale * 0.30).toFixed(0) + 'px');
                t.setAttribute('font-weight',        'bold');
                t.setAttribute('fill',               (tile.rollNum === 6 || tile.rollNum === 8) ? '#c00' : '#111');
                t.setAttribute('pointer-events',     'none');
                t.textContent = tile.rollNum;
                g.appendChild(t);
            }

            // Port indicator (SEA with port)
            if (tile.type === 'SEA' && tile.portType) {
                var portLbl = tile.portType === 'WILDCARD' ? '?' : tile.portType.charAt(0);
                var circ = document.createElementNS(SVG_NS, 'circle');
                circ.setAttribute('cx',            sx.toFixed(1));
                circ.setAttribute('cy',            sy.toFixed(1));
                circ.setAttribute('r',             (scale * 0.23).toFixed(1));
                circ.setAttribute('fill',          '#fff');
                circ.setAttribute('stroke',        '#444');
                circ.setAttribute('stroke-width',  '1.2');
                circ.setAttribute('pointer-events','none');
                g.appendChild(circ);

                var pt = document.createElementNS(SVG_NS, 'text');
                pt.setAttribute('x',                 sx.toFixed(1));
                pt.setAttribute('y',                 sy.toFixed(1));
                pt.setAttribute('text-anchor',       'middle');
                pt.setAttribute('dominant-baseline', 'middle');
                pt.setAttribute('font-size',         (scale * 0.22).toFixed(0) + 'px');
                pt.setAttribute('font-weight',       'bold');
                pt.setAttribute('fill',              '#222');
                pt.setAttribute('pointer-events',    'none');
                pt.textContent = portLbl;
                g.appendChild(pt);
            }

            // Type label (small, near bottom of tile)
            if (tile.type !== 'SEA') {
                var lbl = document.createElementNS(SVG_NS, 'text');
                lbl.setAttribute('x',                 sx.toFixed(1));
                lbl.setAttribute('y',                 (sy + scale * 0.33).toFixed(1));
                lbl.setAttribute('text-anchor',       'middle');
                lbl.setAttribute('dominant-baseline', 'middle');
                lbl.setAttribute('font-size',         (scale * 0.14).toFixed(0) + 'px');
                lbl.setAttribute('fill',              'rgba(0,0,0,0.55)');
                lbl.setAttribute('pointer-events',    'none');
                lbl.textContent = TILE_LABELS[tile.type] || '';
                g.appendChild(lbl);
            }
        }

        // Click / right-click handlers
        (function(k) {
            g.addEventListener('click', function() { paintCell(k); });
            g.addEventListener('contextmenu', function(e) {
                e.preventDefault();
                delete tiles[k];
                render();
            });
        })(key);

        svg.appendChild(g);
    }
}

function paintCell(key) {
    if (selectedType === 'EMPTY') {
        delete tiles[key];
    } else {
        var roll = (selectedType === 'SEA' || selectedType === 'DESERT') ? 0 : selectedRoll;
        var port = (selectedType === 'SEA' && selectedPort !== 'none') ? selectedPort : null;
        tiles[key] = {type: selectedType, rollNum: roll, portType: port};
    }
    render();
}

// ---- Export ----
function buildBoardSpec() {
    var tileList = [];
    for (var key in tiles) {
        var parts = key.split(',');
        var t     = tiles[key];
        var obj   = {x: +parts[0], y: +parts[1], z: +parts[2], type: t.type};
        if (t.rollNum > 0) obj.rollNum  = t.rollNum;
        if (t.portType)    obj.portType = t.portType;
        tileList.push(obj);
    }
    var rawName = document.getElementById('board-name').value || '';
    var name    = rawName.replace(/[^A-Za-z0-9 ]/g, '').trim() || 'Custom';
    return {
        name:                   name,
        suggestedPlayers:       parseInt(document.getElementById('board-players').value) || 4,
        suggestedVictoryPoints: parseInt(document.getElementById('board-vp').value)      || 10,
        tiles:                  tileList
    };
}

function showExportModal() {
    var json = JSON.stringify(buildBoardSpec(), null, 2);
    document.getElementById('export-json-text').value = json;
    $('#export-modal').modal('show');
}

function copyJSON() {
    var ta = document.getElementById('export-json-text');
    ta.select();
    document.execCommand('copy');
    var btn = document.getElementById('copy-json-btn');
    btn.textContent = 'Copied!';
    setTimeout(function() { btn.textContent = 'Copy'; }, 2000);
}

// ---- Play This Board ----
function startGameWithBoard() {
    var spec = buildBoardSpec();
    var json = JSON.stringify(spec);

    if (json.length > 3800) {
        alert('Board is too large to pass as a cookie (' + json.length + ' chars).\nExport the JSON and save it as a scenario file instead.');
        return;
    }

    var userName = document.getElementById('play-username').value.replace(/[^A-Za-z0-9 ]/g, '').trim();
    if (!userName) { alert('Please enter a username.'); return; }

    var gameName   = (document.getElementById('play-game-name').value.replace(/[^A-Za-z0-9 ]/g,'').trim()) || 'Custom Game';
    var numPlayers = document.getElementById('play-players').value;
    var vp         = document.getElementById('play-vp').value;
    var seafarers  = document.getElementById('play-seafarers').checked;

    setCookie('userName',           userName);
    setCookie('numPlayersDesired',  numPlayers);
    setCookie('victoryPoints',      vp);
    setCookie('groupName',          gameName);
    setCookie('boardLayout',        'CUSTOM');
    setCookie('isSeafarers',        seafarers);
    setCookie('customBoardSpec',    json);
    setCookie('isDecimal',          'false');
    setCookie('isDynamic',          'false');
    setCookie('isStandard',         'false');
    setCookie('isSpecialBuildPhase','false');
    deleteCookie('USER_ID');
    deleteCookie('desiredGroupId');

    window.location.href = '/board';
}

function setCookie(name, val) { document.cookie = name + '=' + val; }
function deleteCookie(name)   { document.cookie = name + '=;expires=Thu,01 Jan 1970 00:00:01 GMT;'; }

// ---- Toolbar state ----
function updateSidebar() {
    if (selectedType === 'SEA') {
        $('#roll-group').hide(); $('#port-group').show();
    } else if (selectedType === 'DESERT' || selectedType === 'EMPTY') {
        $('#roll-group').hide(); $('#port-group').hide();
    } else {
        $('#roll-group').show(); $('#port-group').hide();
    }
}

// ---- Init ----
$(document).ready(function() {
    gridCells = generateGrid(7);

    var svg = document.getElementById('editor-svg');

    // Defer offset calculation until layout is complete
    setTimeout(function() {
        offsetX = svg.clientWidth  / 2;
        offsetY = svg.clientHeight / 2;
        render();
    }, 50);

    // Zoom
    svg.addEventListener('wheel', function(e) {
        e.preventDefault();
        scale = Math.max(25, Math.min(130, scale + (e.deltaY < 0 ? 5 : -5)));
        render();
    }, {passive: false});

    // Pan (middle mouse button)
    var panning = false, px = 0, py = 0;
    svg.addEventListener('mousedown', function(e) {
        if (e.button === 1) {
            panning = true; px = e.clientX; py = e.clientY;
            e.preventDefault();
        }
    });
    window.addEventListener('mousemove', function(e) {
        if (panning) {
            offsetX += e.clientX - px; offsetY += e.clientY - py;
            px = e.clientX; py = e.clientY;
            render();
        }
    });
    window.addEventListener('mouseup', function(e) {
        if (e.button === 1) panning = false;
    });

    // Tile type buttons
    $(document).on('click', '.tile-type-btn', function() {
        selectedType = $(this).data('type');
        $('.tile-type-btn').removeClass('active');
        $(this).addClass('active');
        updateSidebar();
    });

    $('#roll-select').on('change', function() { selectedRoll = +$(this).val(); });
    $('#port-select').on('change', function() { selectedPort = $(this).val(); });

    updateSidebar();
});
