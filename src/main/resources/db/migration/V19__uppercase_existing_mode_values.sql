UPDATE homepage_section_configs 
SET mode = UPPER(mode) 
WHERE mode != UPPER(mode);