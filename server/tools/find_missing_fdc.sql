SELECT name_de, id, energy_kcal_per_100g
FROM ingredients 
WHERE source='USDA_FDC' 
AND (
    LOWER(name_de) LIKE '%essig%' 
    OR LOWER(name_de) LIKE '%beeren%' 
    OR LOWER(name_de) LIKE '%brühe%' 
    OR LOWER(name_de) LIKE '%joghurt%' 
    OR LOWER(name_de) LIKE '%tofu%' 
    OR LOWER(name_de) LIKE '%hackfleisch%' 
    OR LOWER(name_de) LIKE '%möhre%' 
    OR LOWER(name_de) LIKE '%karotte%' 
    OR LOWER(name_de) LIKE '%kartoffelmehl%' 
    OR LOWER(name_de) LIKE '%stärke%'
    OR LOWER(name_de) LIKE '%rind%'
)
ORDER BY name_de;
