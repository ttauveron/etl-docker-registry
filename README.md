# ETL Docker Registry

This tool allows to target a docker registry hostname and dump its data in a sqlite database generated file.

It currently works for Nexus docker registry, might need adjustments for others.

I developed this tool to analyze a docker registry in order to setup a cleanup strategy.

### How to run
#### Command line arguments
```
Usage: docker-registry-etl [-hV] [--basic-auth=<basicAuth>] [-i=FILE]
                           -r=<registryHost> [-t=<numThreads>]
Dumps data from a docker registry in a sqlite database generated file.
      --basic-auth=<basicAuth>
                      Base64 encoded 'username:password' String
  -h, --help          Show this help message and exit.
  -i, --images=FILE   File containing a list of images to be scanned, with the
                        format 'image:tag' on each line
  -r, --registry=<registryHost>
                      Hostname for the docker registry (without http[s]://)
  -t, --threads=<numThreads>
                      Number of concurrent threads to be used
  -V, --version       Print version information and exit.
```

The only mandatory option is `--registry`. 

Some docker registries don't allow to query catalog/tag so we can add the `--images` option to provide a list of image:tag to be scanned.

#### Run with docker
```shell 
touch docker-registry.db && \
    docker run --rm -v $(pwd)/docker-registry.db:/docker-registry.db \
    quay.io/ttauveron/etl-docker-registry \
    -r my.registry.hostname \
    -t 30
```

### Example SQL
#### Get shared layers
```sql
select
       lm1.image_digest as image_digest_1,
       lm2.image_digest as image_digest_2,
       lm1.layer_digest as shared_layer_digest
from layer_manifest lm1
join layer_manifest lm2
on lm1.image_digest != lm2.image_digest
and lm1.layer_digest = lm2.layer_digest
where image_digest_1 > image_digest_2
order by shared_layer_digest, image_digest_1 desc;
```

#### Disk space that could be saved removing a particular tag
In practise, deleting those tags saves less space than what's returned by this query. I don't know why there is a mismatch yet. 
```sql
WITH image_unshared_layers as (
select
       m.image_digest,
       l.digest as layer_digest,
       sum(l.size)/1024/1024 as mib
from manifests m
join layer_manifest lm on m.image_digest = lm.image_digest
join layers l on lm.layer_digest = l.digest
where l.digest not in (
    select
       lm1.layer_digest as shared_layer_digest
    from layer_manifest lm1
    join layer_manifest lm2
    on lm1.image_digest != lm2.image_digest
    and lm1.layer_digest = lm2.layer_digest
    where lm1.image_digest > lm2.image_digest
    )
group by m.image_digest, l.digest
),
image_tag_unique as (
    select
           i.name,
           i.tag,
           i.digest as digest,
           COUNT(i.digest) as nb_other_tags_for_image
    from images i
    group by i.digest
    having i.digest = MIN(i.digest)
)
select
       i.name || ':' || i.tag as image,
       i.nb_other_tags_for_image,
       sum(lm.mib) as mib
from image_tag_unique i
join image_unshared_layers lm
on lm.image_digest = i.digest
group by i.name, i.tag, i.nb_other_tags_for_image
order by mib desc;
```

#### List of tags to delete in order to keep only the 5 most recent

```sql
with images_created_modified as (
-- images created / last modified
select
       i.name as image,
       i.tag as tag,
       datetime(m.created) as created,
       max(datetime(l.last_modified)) as last_modified,
       ROW_NUMBER()  OVER (
           PARTITION BY i.name
           ORDER BY datetime(l.last_modified) desc
           ) as top_recent
from layers l
join layer_manifest lm on l.digest = lm.layer_digest
join manifests m on lm.image_digest = m.image_digest
join images i on m.image_digest = i.digest
group by m.image_digest, i.name, i.tag
)
select
       i.name,
       i.tag
from images i
where not exists (
    select 1
    from images_created_modified icm
    where icm.top_recent <= 5
    and icm.image = i.name
    and icm.tag = i.tag
)
order by i.name;
```
